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
                #
                # Why `verify` instead of `package`: jacoco-maven-plugin's
                # `report` goal is bound to the verify phase, not package.
                # SonarQube reads target/site/jacoco/jacoco.xml at analysis
                # time, so the report has to exist before that stage runs.
                # `mvn package` would skip the report and the quality gate
                # would fail with `new_coverage = 0%` because the JaCoCo
                # XML Report Importer sensor finds nothing to import.
                # `-DskipITs` keeps the `*IT.java` integration tests out
                # of the verify phase even though it's now in scope.
                mvn -B clean verify -DskipITs -Dtest="!CleanupBatchIntegrationTest,!HibernateBatchPerformanceTest,!RetryBatchIntegrationTest,!PactProviderVerificationTest,!PostApiKarateTest"
                '''
            }
        }
        stage('SonarQube analysis') {
            when { expression { params.MODE == 'ci' || params.MODE == 'both' } }
            steps {
                script {
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

                        // ---- Quality Gate check (curl-based) ----
                        // We deliberately do NOT use `waitForQualityGate` here. The
                        // plugin's OkHttp client hits a JDK 21 + glibc bug in the
                        // Socket.connect(unresolved_addr) async DNS path
                        // (UnknownHostException for any unresolved hostname/IP),
                        // which breaks the gate-poll request. Using curl (a
                        // subprocess) sidesteps the JVM and works fine.
                        //
                        // Status semantics from /api/qualitygates/project_status:
                        //   OK    -> pass
                        //   WARN  -> yellow, mark unstable but continue
                        //   ERROR -> red, fail the build
                        //   PENDING / NONE -> still being evaluated, keep polling
                        def qgStatus = ''
                        timeout(time: 5, unit: 'MINUTES') {
                            waitUntil {
                                qgStatus = sh(
                                    script: 'curl -fsS -u "${SONAR_AUTH_TOKEN}:" "${SONAR_HOST_URL}/api/qualitygates/project_status?projectKey=post-api" | jq -r \'.projectStatus.status\'',
                                    returnStdout: true
                                ).trim()
                                echo "SonarQube Quality Gate status: ${qgStatus}"
                                return ['OK', 'WARN', 'ERROR'].contains(qgStatus)
                            }
                        }
                        if (qgStatus == 'ERROR') {
                            error("SonarQube Quality Gate failed (status=${qgStatus})")
                        } else if (qgStatus == 'WARN') {
                            unstable("SonarQube Quality Gate warning (status=${qgStatus})")
                        }
                    }
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
                # Kill any leftover port-forward from the Karate stage
                # (its `trap kill ... EXIT` should have done this, but a
                # backgrounded `&` process can outlive the sh step that
                # spawned it, so be explicit). Then start a fresh
                # port-forward and wait until the API actually responds
                # before launching Playwright. The previous version's
                # `sleep 5` was a race — port-forward isn't always ready
                # when Playwright probes webServer.port=8083, and the
                # placeholder `webServer.command='echo ...'` exits
                # immediately, leading to "Process from config.webServer
                # exited early" and an aborted test run.
                pkill -f "kubectl port-forward.*${params.NAMESPACE}" 2>/dev/null || true
                kubectl -n ${params.NAMESPACE} port-forward deployment/post-api 8083:8081 >/dev/null 2>&1 &
                PF_PID=\$!
                # Wait up to 60s for the port-forward to actually be serving the API.
                for i in \$(seq 1 30); do
                    if curl -sf http://localhost:8083/api/posts/published?size=1 >/dev/null 2>&1; then
                        echo "Port-forward ready (try \$i)"
                        break
                    fi
                    sleep 2
                done
                trap "kill \$PF_PID 2>/dev/null || true" EXIT
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
