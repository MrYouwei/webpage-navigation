pipeline {
    agent any
    stages {
        stage('拉取代码') {
            steps {
                checkout scm
            }
        }
        stage('后端SpringBoot Maven打包') {
            steps {
                dir("backend") {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }
        stage('归档Jar产物') {
            steps {
                archiveArtifacts artifacts: 'backend/target/*.jar', fingerprint: true
            }
        }
    }
}
