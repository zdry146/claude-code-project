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
                mkdir -p post-api/src/main/resources/static
                cp -r post-api-frontend/dist/* post-api/src/main/resources/static/
                # Run the full test suite (Junit + Pact + Karate)
                cd post-api
                mvn -B clean verify
                '''
            }
        }
        stage('Determine image version') {
            when { expression { params.MODE == 'ci' || params.MODE == 'both' } }
            steps {
                script {
                    env.IMAGE_VERSION = sh(
                        label: 'Read project version',
                        script: 'mvn -B -q -DforceStdout -Dexpression=project.version -f post-api/pom.xml help:evaluate',
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
                cd post-api
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
        stage('Ensure image pull secret') {
            when { expression { params.MODE == 'cd' || params.MODE == 'both' } }
            steps {
                sh '''
                set -euo pipefail
                kubectl -n ${params.NAMESPACE} create secret docker-registry aliyun-registry-cred \
                    --docker-server=${env.ALIYUN_REGISTRY} \
                    --docker-username=${ALIYUN_DOCKER_CREDS_USR} \
                    --docker-password=${ALIYUN_DOCKER_CREDS_PSW} \
                    --dry-run=client -o yaml | kubectl apply -f -
                '''
            }
        }
        stage('Apply manifests') {
            when { expression { params.MODE == 'cd' || params.MODE == 'both' } }
            steps {
                sh '''
                set -euo pipefail
                kubectl apply -f post-api/k8s/namespace.yaml
                # db-credentials Secret is generated from the Jenkins 'db-password' credential at deploy time.
                kubectl -n ${params.NAMESPACE} create secret generic db-credentials \
                    --from-literal=password="${DB_PASSWORD}" \
                    --dry-run=client -o yaml | kubectl apply -f -
                # Substitute __DB_HOST__ / __DB_DATABASE__ / __IMAGE_TAG__ placeholders
                sed -e "s|__DB_HOST__|${params.DB_HOST}|g" \
                    -e "s|__DB_DATABASE__|${params.DB_DATABASE}|g" \
                    -e "s|__IMAGE_TAG__|${env.DEPLOY_TAG}|g" \
                    post-api/k8s/deployment.yaml | kubectl -n ${params.NAMESPACE} apply -f -
                '''
            }
        }
        stage('Wait for ready') {
            when { expression { params.MODE == 'cd' || params.MODE == 'both' } }
            steps {
                sh '''
                set -euo pipefail
                kubectl -n ${params.NAMESPACE} rollout status deployment/post-api --timeout=180s
                '''
            }
        }
        stage('Initialize DB schema + seed data') {
            when { expression { params.MODE == 'cd' || params.MODE == 'both' } }
            steps {
                sh '''
                set -euo pipefail
                # Run schema-postgres.sql + data.sql against the cluster-reachable
                # PostgreSQL server. The Jenkins host (or a sidecar pod with
                # psql installed) is used to avoid the brittle "psql inside
                # post-api pod" path.
                #
                # Uses PGPASSWORD env var injected from the db-password
                # Jenkins credential (DB_PASSWORD). psql must be on PATH.
                export PGPASSWORD="$DB_PASSWORD"
                PSQL="psql -h ${params.DB_HOST} -p 5432 -U postgres -d ${params.DB_DATABASE} -v ON_ERROR_STOP=1"
                echo "Applying schema..."
                # Filter out the BATCH_JOB_SEQ lines that use deprecated
                # NO CACHE NO CYCLE syntax (PostgreSQL 17+ rejects this).
                grep -v "BATCH_JOB" post-api/src/main/resources/schema-postgres.sql | $PSQL
                echo "Truncating and re-seeding posts..."
                $PSQL -c "TRUNCATE TABLE posts RESTART IDENTITY CASCADE;" || true
                $PSQL -f post-api/src/main/resources/data.sql
                echo "DB schema and seed data initialized"
                '''
            }
        }
        stage('E2E: Karate API tests') {
            when { expression { params.MODE == 'cd' || params.MODE == 'both' } }
            steps {
                sh '''
                set -euo pipefail
                # Port-forward to the running deployment
                kubectl -n ${params.NAMESPACE} port-forward deployment/post-api 8083:8081 &
                PF_PID=$!
                sleep 8
                trap "kill $PF_PID 2>/dev/null || true" EXIT

                # Wait for API to be ready
                for i in $(seq 1 30); do
                    if curl -sf http://localhost:8083/api/posts/published?size=1 >/dev/null 2>&1; then
                        echo "API is ready"
                        break
                    fi
                    echo "Waiting for API... ($i)"
                    sleep 3
                done

                cd post-api
                mvn -B test -Dtest=PostApiKarateTest -DbaseUrl=http://localhost:8083
                '''
            }
        }
        stage('E2E: Playwright UI tests') {
            when { expression { params.MODE == 'cd' || params.MODE == 'both' } }
            steps {
                sh '''
                set -euo pipefail
                # Reuses the port-forward from the Karate stage if still alive
                if ! pgrep -f "kubectl port-forward.*${params.NAMESPACE}" >/dev/null; then
                    kubectl -n ${params.NAMESPACE} port-forward deployment/post-api 8083:8081 &
                    PF_PID=$!
                    sleep 5
                    trap "kill $PF_PID 2>/dev/null || true" EXIT
                fi
                # Run Playwright tests against the embedded React UI (already built in Build stage)
                cd post-api-frontend
                npx playwright test --reporter=list
                '''
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
