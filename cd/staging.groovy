@Library('jenkins-shared-lib@dev') _

pipeline {
    agent any  // деплой по SSH с контроллера, отдельная нода не требуется

    parameters {
        string(name: 'IMAGE_TAG', description: 'Тег образа для деплоя (SHA коммита)')
    }

    stages {
        stage('Deploy to staging') {
            steps {
                withEnv(["IMAGE_TAG=${params.IMAGE_TAG}"]) {
                    sshagent(['debian-vm-ssh']) {
                        sh '''
                            set -e
                            echo "=== Deploying to staging ==="
                            ssh -o StrictHostKeyChecking=no vboxuser@192.168.56.101 "mkdir -p /home/vboxuser/voidsounds"
                            scp -o StrictHostKeyChecking=no docker-compose.staging.yml vboxuser@192.168.56.101:/home/vboxuser/voidsounds/
                            ssh -o StrictHostKeyChecking=no vboxuser@192.168.56.101 "cd /home/vboxuser/voidsounds && docker pull ghcr.io/clementineqq/voidsounds:${IMAGE_TAG} && IMAGE_TAG=\${IMAGE_TAG} docker compose -f docker-compose.staging.yml up -d"
                            # ---------- ЭТАП 5: HEALTHCHECK ----------
                            ssh -o StrictHostKeyChecking=no vboxuser@192.168.56.101 "for i in \$(seq 1 30); do if curl -sf http://localhost:8080/healthz; then echo 'Приложение здорово'; exit 0; fi; sleep 2; done; echo 'Приложение не отвечает на healthz — деплой не удался'; exit 1"
                        '''
                    } //TODO: StrictHostKeyChecking=no потом исправить на known_hosts
                }
            }
        }
    }

    post {
        success { notify(status: 'success', message: "Deployed :${params.IMAGE_TAG} to staging") }
        failure { notify(status: 'failure', message: "Staging deploy FAILED for :${params.IMAGE_TAG}") }
    }
}
