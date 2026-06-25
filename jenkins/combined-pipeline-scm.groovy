// Combined CI/CD pipeline for the post-api project.
// MODE=ci   → build + test + push image
// MODE=cd   → deploy to k8s (uses IMAGE_TAG parameter)
// MODE=both → ci then cd in one run (uses version from pom.xml)
//
// Pipeline from SCM: pulled into Jenkins from the repository at
// job-creation time. Credentials used: aliyun-docker-login, db-password.
pipeline {
    agent any
    options {
        timeout(time: 45, unit: 'MINUTES')
    }
    tools {
        maven 'maven'
        jdk 'java'
    }
    parameters {
        choice(
            name: 'MODE',
            choices: ['ci', 'cd', 'both'],
            description: 'ci = build+test+push, cd = deploy to k8s, both = ci then cd in one run'
        )
        choice(
            name: 'IMAGE_TAG',
            choices: ['latest', '1.0.0'],
            description: 'Image tag to deploy (cd mode only; ignored when MODE=both or ci)'
        )
        string(
            name: 'NAMESPACE',
            defaultValue: 'post-api',
            description: 'Kubernetes namespace (cd mode only)'
        )
        string(
            name: 'DB_HOST',
            defaultValue: '192.168.232.128',
            description: 'PostgreSQL host (cluster-reachable IP/hostname) injected as the SPRING_DATASOURCE_URL (cd mode only).'
        )
        string(
            name: 'DB_DATABASE',
            defaultValue: 'testdb',
            description: 'PostgreSQL database name (cd mode only).'
        )
    }
    environment {
        ALIYUN_REGISTRY     = 'crpi-e2h2rfj3kunrwe5n.cn-hangzhou.personal.cr.aliyuncs.com'
        ALIYUN_NAMESPACE    = 'mike-docker-registry'
        ALIYUN_IMAGE        = 'post-api'
        FULL_IMAGE          = "${env.ALIYUN_REGISTRY}/${env.ALIYUN_NAMESPACE}/${env.ALIYUN_IMAGE}"
        ALIYUN_DOCKER_CREDS = credentials('aliyun-docker-login')
        DB_PASSWORD         = credentials('db-password')
        FRONTEND_GIT_CREDS  = credentials('git-cred')
        // Construct the full authenticated Git URL at runtime
    }
    stages {
        stage('Build & test') {
            when { expression { params.MODE == 'ci' || params.MODE == 'both' } }
            steps {
                sh '''
                set -euo pipefail
                # Frontend is part of this monorepo (post-api-frontend/)
                # Build the React frontend (vite build → dist/)
                cd post-api-frontend
                npm install --silent
                npx vite build
                cd ..
                # Embed the React frontend dist/ into the Spring Boot jar
                mkdir -p src/main/resources/static
                cp -r post-api-frontend/dist/* src/main/resources/static/
                # Run the full test suite (Junit + Pact + Karate)
                # NOTE: PactProviderVerificationTest is excluded until the PACT
                # Broker is wired up; flip the ! prefix off once that lands.
                mvn -B clean package -DskipITs -Dtest="!CleanupBatchIntegrationTest,!HibernateBatchPerformanceTest,!RetryBatchIntegrationTest,!PactProviderVerificationTest,!PostApiKarateTest"
                '''
            }
        }
        stage('SonarQube analysis') {
            when { expression { params.MODE == 'ci' || params.MODE == 'both' } }
            steps {
                withSonarQubeEnv('local-sonarqube') {
                    // withSonarQubeEnv injects SONAR_HOST_URL + SONAR_AUTH_TOKEN
                    // from the 'local-sonarqube' installation in
                    // Manage Jenkins > System > SonarQube servers.
                    sh '''
                    set -euo pipefail
                    mvn -B sonar:sonar \
                      -Dsonar.projectKey=post-api \
                      -Dsonar.projectName='Post API' \
                      -Dsonar.exclusions='src/main/resources/static/**,post-api-frontend/**,target/**,**/*.min.js'
                    '''
                }
                // Block until the SonarQube Quality Gate is evaluated.
                // Fails the build if the gate is RED. Tune wait time as needed.
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        stage('Determine image version') {
            when { expression { params.MODE == 'ci' || params.MODE == 'both' } }
            steps {
                script {
                    env.IMAGE_VERSION = sh(
                        label: 'Read project version',
                        script: 'mvn -B -q -DforceStdout -Dexpression=project.version help:evaluate',
                        returnStdout: true
                    ).trim()
                }
            }
        }
        stage('Build & push image') {
            when { expression { params.MODE == 'ci' || params.MODE == 'both' } }
            steps {
                sh '''
                set -euo pipefail
                echo "$ALIYUN_DOCKER_CREDS_PSW" | docker login -u "$ALIYUN_DOCKER_CREDS_USR" --password-stdin "$ALIYUN_REGISTRY"
                docker build -t "$FULL_IMAGE:$IMAGE_VERSION" -t "$FULL_IMAGE:latest" .
                docker push "$FULL_IMAGE:$IMAGE_VERSION"
                docker push "$FULL_IMAGE:latest"
                '''
            }
        }
        stage('Resolve deploy tag') {
            when { expression { params.MODE == 'cd' || params.MODE == 'both' } }
            steps {
                script {
                    env.DEPLOY_TAG = (params.MODE == 'both') ? env.IMAGE_VERSION : params.IMAGE_TAG
                    echo "Deploying tag: ${env.DEPLOY_TAG} (MODE=${params.MODE})"
                }
            }
        }
        stage('Ensure namespace + image pull secret') {
            when { expression { params.MODE == 'cd' || params.MODE == 'both' } }
            steps {
                // Create the namespace first (idempotent) so subsequent
                // `kubectl -n ${params.NAMESPACE} ...` commands succeed.
                sh """
                set -euo pipefail
                kubectl apply -f k8s/namespace.yaml
                """
                withCredentials([
                    usernamePassword(
                        credentialsId: 'aliyun-docker-login',
                        usernameVariable: 'ALIYUN_DOCKER_CREDS_USR',
                        passwordVariable: 'ALIYUN_DOCKER_CREDS_PSW'
                    )
                ]) {
                    // secrets stay as bash vars (\$VAR) so they're never interpolated by Groovy
                    sh """
                    set -euo pipefail
                    kubectl -n ${params.NAMESPACE} create secret docker-registry aliyun-registry-cred \\
                        --docker-server=${env.ALIYUN_REGISTRY} \\
                        --docker-username=\$ALIYUN_DOCKER_CREDS_USR \\
                        --docker-password=\$ALIYUN_DOCKER_CREDS_PSW \\
                        --dry-run=client -o yaml | kubectl apply -f -
                    """
                }
            }
        }
        stage('Apply manifests') {
            when { expression { params.MODE == 'cd' || params.MODE == 'both' } }
            steps {
                // DB password stays a bash var via withCredentials so Groovy
                // never sees the plaintext (and no GString-interpolation warning).
                withCredentials([string(credentialsId: 'db-password', variable: 'DB_PASSWORD')]) {
                    sh """
                    set -euo pipefail
                    # db-credentials Secret is generated from the Jenkins 'db-password' credential at deploy time.
                    kubectl -n ${params.NAMESPACE} create secret generic db-credentials \\
                        --from-literal=password="\$DB_PASSWORD" \\
                        --dry-run=client -o yaml | kubectl apply -f -
                    # Substitute __DB_HOST__ / __DB_DATABASE__ / __IMAGE_TAG__ placeholders
                    sed -e "s|__DB_HOST__|${params.DB_HOST}|g" \\
                        -e "s|__DB_DATABASE__|${params.DB_DATABASE}|g" \\
                        -e "s|__IMAGE_TAG__|${env.DEPLOY_TAG}|g" \\
                        k8s/deployment.yaml | kubectl -n ${params.NAMESPACE} apply -f -
                    """
                }
            }
        }
        stage('Wait for ready') {
            when { expression { params.MODE == 'cd' || params.MODE == 'both' } }
            steps {
                sh """
                set -euo pipefail
                kubectl -n ${params.NAMESPACE} rollout status deployment/post-api --timeout=180s
                """
            }
        }
        // Schema + seed data initialization moved into the application:
        //   com.example.postapi.config.SchemaInitializer (CommandLineRunner)
        // runs schema-postgres.sql + data.sql on first boot via JdbcTemplate.
        stage('E2E: Karate API tests') {
            when { expression { params.MODE == 'cd' || params.MODE == 'both' } }
            steps {
                sh """
                set -euo pipefail
                # Port-forward to the running deployment
                kubectl -n ${params.NAMESPACE} port-forward deployment/post-api 8083:8081 &
                PF_PID=\$!
                sleep 8
                trap "kill \$PF_PID 2>/dev/null || true" EXIT

                # Wait for API to be ready
                for i in \$(seq 1 30); do
                    if curl -sf http://localhost:8083/api/posts/published?size=1 >/dev/null 2>&1; then
                        echo "API is ready"
                        break
                    fi
                    echo "Waiting for API... (\$i)"
                    sleep 3
                done

                mvn -B test -Dtest=PostApiKarateTest -DbaseUrl=http://localhost:8083
                """
            }
        }
        stage('E2E: Playwright UI tests') {
            when { expression { params.MODE == 'cd' || params.MODE == 'both' } }
            steps {
                sh """
                set -euo pipefail
                # Reuses the port-forward from the Karate stage if still alive
                if ! pgrep -f "kubectl port-forward.*${params.NAMESPACE}" >/dev/null; then
                    kubectl -n ${params.NAMESPACE} port-forward deployment/post-api 8083:8081 &
                    PF_PID=\$!
                    sleep 5
                    trap "kill \$PF_PID 2>/dev/null || true" EXIT
                fi
                # Install frontend deps: this stage runs in MODE=cd / both, where
                # the 'Build & test' stage (which would have run npm install) is
                # skipped. So we need to npm install here too.
                cd post-api-frontend
                npm install --silent
                # Use the host's already-installed chromium instead of having
                # Playwright download its bundled browser. The host kernel is
                # 7.0.0-generic (Ubuntu 26.04), which Playwright 1.58.2 does
                # not support for downloading. Host has /snap/bin/chromium.
                export PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
                export PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH=/snap/bin/chromium
                npx playwright test --reporter=list
                """
            }
        }
    }
    post {
        success {
            echo "Pipeline (MODE=${params.MODE}) succeeded"
        }
        failure {
            echo "Pipeline (MODE=${params.MODE}) failed"
            script {
                if (params.MODE == 'cd' || params.MODE == 'both') {
                    sh "kubectl -n ${params.NAMESPACE} get all || true"
                }
            }
        }
        always {
            sh "pkill -f 'kubectl port-forward.*${params.NAMESPACE}' 2>/dev/null || true"
        }
    }
}
