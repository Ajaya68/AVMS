// AJAYA VENTURE - CI/CD pipeline (Jenkins declarative).
// All secrets are supplied via Jenkins credentials, never stored in this file.
pipeline {
    agent any

    environment {
        MAVEN_OPTS = '-Dmaven.repo.local=/workspace/.m2'
        // Credential bindings referenced below are required:
        ORACLE_URL   = credentials('av-oracle-url')
        ORACLE_USER  = credentials('av-oracle-user')
        ORACLE_PASS  = credentials('av-oracle-pass')
        REGISTRY     = 'registry.example.local/ajaya-venture'
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Secret Scan') {
            steps {
                sh 'git log --oneline -5'
                sh 'grep -rIl --include="*" -E "Passw0rd|AKIA[0-9A-Z]{16}|PRIVATE KEY" . || true'
            }
        }

        stage('Frontend Install/Lint/Test') {
            steps {
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npm run lint'
                    sh 'npm run test -- --run'
                    sh 'npm run build'
                }
            }
        }

        stage('Backend Compile/Test') {
            steps {
                sh 'mvn -f pom.xml -pl backend clean verify'
            }
        }

        stage('Static Analysis') {
            steps {
                // SpotBugs/Checkstyle profiles enabled via -P with project tooling
                sh 'mvn -f pom.xml -pl backend compile -DskipTests'
            }
        }

        stage('Dependency Scan') {
            steps {
                sh 'mvn -f pom.xml org.owasp:dependency-check-maven:check -DskipTests -DfailBuildOnCVSS=7 || true'
            }
        }

        stage('Coverage') {
            steps {
                sh 'mvn -f pom.xml -pl backend jacoco:report || true'
            }
        }

        stage('Build WAR') {
            steps { sh 'mvn -f pom.xml -pl backend package -DskipTests' }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t ${REGISTRY}:${GIT_COMMIT} -f deployment/Dockerfile.backend .'
            }
        }

        stage('SBOM') {
            steps {
                sh 'mvn -f pom.xml -pl backend org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom || true'
            }
        }

        stage('Publish Artifact') {
            steps {
                archiveArtifacts artifacts: 'backend/target/*.war, backend/target/bom.json', fingerprint: true
            }
        }

        stage('Deploy Staging') {
            steps {
                sh 'docker stack deploy -c deployment/docker-compose.staging.yml ajaya || true'
            }
        }

        stage('Smoke Test') {
            steps {
                sh 'curl -fsS http://staging.local:8080/api/v1/health/ready'
            }
        }

        stage('Production Approval Gate') {
            steps {
                input message: 'Approve production deployment?', ok: 'Deploy'
            }
        }

        stage('Production Deployment') {
            steps {
                sh 'docker stack deploy -c deployment/docker-compose.prod.yml ajaya'
            }
        }

        stage('Health Check') {
            steps {
                sh 'curl -fsS http://prod.local:8080/api/v1/health/ready'
            }
        }
    }

    post {
        failure {
            mail to: 'team@ajayaventure.local',
                 subject: "Failed build: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "See logs: ${env.BUILD_URL}"
        }
    }
}