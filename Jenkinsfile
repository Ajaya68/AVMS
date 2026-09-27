pipeline {
  agent any
  environment {
    JAVA_HOME = tool 'jdk-17'
  }
  stages {
    stage('Checkout') { steps { checkout scm } }
    stage('Backend verify') {
      steps { sh 'mvn -B -f backend-java/pom.xml verify' }
    }
    stage('SonarQube') {
      steps { withSonarQubeEnv('sonarqube') { sh 'mvn -B -f backend-java/pom.xml sonar:sonar' } }
    }
    stage('Frontend build') {
      steps { sh 'npm --prefix frontend ci && npm --prefix frontend run lint && npm --prefix frontend run build' }
    }
    stage('Docker build') {
      steps { sh 'docker build -t avms-backend:${BUILD_NUMBER} backend-java' }
    }
    stage('JMeter smoke') {
      steps { sh 'jmeter -n -t tests/jmeter/smoke.jmx -l smoke.jtl || true' }
    }
  }
  post {
    always { junit 'backend-java/target/surefire-reports/*.xml' }
  }
}
