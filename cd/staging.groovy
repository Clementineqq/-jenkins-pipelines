@Library('jenkins-shared-lib@dev') _

pipeline {
    agent { label 'deploy' }            // нода на целевой ВМ, с докером

    parameters {
        string(name: 'IMAGE_TAG', description: 'Тег образа для деплоя (SHA коммита)')
    }

    environment {
        REGISTRY = 'ghcr.io'
        APP_NAME = 'clementineqq/voidsounds'   
    }

    stages {
        stage('Deploy to staging') {
            steps {
                // пробрасываем тег в окружение, иначе docker-compose его не увидит
                withEnv(["IMAGE_TAG=${params.IMAGE_TAG}"]) {
                    sh '''
                        docker pull ${REGISTRY}/${APP_NAME}:${IMAGE_TAG}
                        docker compose -f docker-compose.staging.yml up -d
                    '''
                }
            }
        }
    }

    post {
        success { notify(status: 'success', message: "Deployed :${params.IMAGE_TAG} to staging") }
        failure { notify(status: 'failure', message: "Staging deploy FAILED for :${params.IMAGE_TAG}") }
    }
}
