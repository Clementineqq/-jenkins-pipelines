@Library('jenkins-shared-lib@dev') _

pipeline {
    agent any // крутится на контроллере, ssh ходит наружу

    parameters {
        string(name: 'IMAGE_TAG', description: 'Тег образа для деплоя (SHA коммита)')
    }

    stages {
        stage('Deploy to staging') {
            steps {
                withEnv(["IMAGE_TAG=${params.IMAGE_TAG}"]) {   // кладём тег в окружение
                    sshagent(['debian-vm-ssh']) {                    // ID ключа из Credentials
                        sh '''
                            set -e
                            ssh -o StrictHostKeyChecking=no vboxuser@192.168.56.101 \\
                                "mkdir -p /home/vboxuser/voidsounds"
                            scp -o StrictHostKeyChecking=no \\
                                docker-compose.staging.yml \\
                                vboxuser@192.168.56.101:/home/vboxuser/voidsounds/
                            ssh -o StrictHostKeyChecking=no vboxuser@192.168.56.101 \\
                                "cd /home/vboxuser/voidsounds && \\
                                 docker pull ghcr.io/clementineqq/voidsounds:${IMAGE_TAG} && \\
                                 IMAGE_TAG=${IMAGE_TAG} docker compose -f docker-compose.staging.yml up -d"
                        '''
                    } //TODO: StrictHostKeyChecking=no потом исправить на knwon_hosts
                }
            }
        }
    }

    post {
        success { notify(status: 'success', message: "Deployed :${params.IMAGE_TAG} to staging") }
        failure { notify(status: 'failure', message: "Staging deploy FAILED for :${params.IMAGE_TAG}") }
    }
}
