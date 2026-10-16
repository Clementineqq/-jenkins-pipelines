@Library('jenkins-shared-lib@dev') _

pipeline {
    agent any  // деплой по SSH с контроллера, отдельная нода не требуется

    parameters {
        string(name: 'IMAGE_TAG', description: 'Тег образа для продакшена (SHA коммита)')
    }

    environment {
        REGISTRY = 'ghcr.io'
        APP_NAME = 'clementineqq/voidsounds'   
    }

    stages {
        stage('Approve Production Deploy') {
            steps {
                input message: "Подтвердить деплой ${APP_NAME}:${params.IMAGE_TAG} в продакшен?", ok: 'Deploy', submitter: 'admin'
            }
        }

        stage('Deploy to production') {
            steps {
                withEnv(["IMAGE_TAG=${params.IMAGE_TAG}"]) { // пробрасываем тег в окружение, иначе docker-compose на VM его не подставит

                    // секреты из Jenkins Credentials
                    withCredentials([
                        string(credentialsId: 'prod-db-password', variable: 'DB_PASSWORD'),
                        string(credentialsId: 'prod-session-secret', variable: 'SESSION_SECRET')
                    ]) {
                        sh '''
                            set -e
                            echo "=== Deploying ${APP_NAME}:${IMAGE_TAG} to production ==="
                            
                            # доставляем compose-файл на целевую ВМ. TODO: StrictHostKeyChecking потом на known_hosts заменить
                            scp -o StrictHostKeyChecking=no \\
                                docker-compose.production.yml \\
                                vboxuser@192.168.56.101:/home/vboxuser/voidsounds/
                            
                            # запускаем на ВМ. TODO: DB_PASSWORD=${DB_PASSWORD} потом через .env сделать
                            ssh -o StrictHostKeyChecking=no vboxuser@192.168.56.101 \\
                                "cd /home/vboxuser/voidsounds && \\
                                 docker pull ${REGISTRY}/${APP_NAME}:${IMAGE_TAG} && \\
                                 IMAGE_TAG=${IMAGE_TAG} DB_PASSWORD=${DB_PASSWORD} SESSION_SECRET=${SESSION_SECRET} \\
                                 docker compose -f docker-compose.production.yml up -d"


                                 # HEALTHCHECK
                            ssh -o StrictHostKeyChecking=no vboxuser@192.168.56.101 \
                                "for i in \$(seq 1 30); do \
                                    if curl -sf http://localhost:8080/healthz; then \
                                        echo \"Приложение здорово\"; \
                                        exit 0; \
                                    fi; \
                                    sleep 2; \
                                done; \
                                echo \"Приложение не отвечает на healthz — деплой не удался\"; \
                                exit 1"
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            notify(status: 'success', message: "Deployed ${APP_NAME}:${params.IMAGE_TAG} to production")
        }
        failure {
            notify(status: 'failure', message: "Production deploy FAILED for ${APP_NAME}:${params.IMAGE_TAG}")
        }
    }
}
