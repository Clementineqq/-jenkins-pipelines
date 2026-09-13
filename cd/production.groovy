def call(Map config = [:]) {
    def appName = config.appName ?: 'voidsounds'
    def registry = config.registry ?: 'ghcr.io'

    pipeline {
        agent { label 'deploy' }

        parameters {
            string(name: 'IMAGE_TAG', description: 'Which image tag to deploy')
        }

        stages {
            stage('Approval') {
                steps {
                    input message: "Deploy ${appName}:${params.IMAGE_TAG} to production?", ok: 'Deploy'
                }
            }

            stage('Deploy') {
                steps {
                    sh """
                        docker pull ${registry}/${appName}:${params.IMAGE_TAG}
                        docker-compose -f docker-compose.prod.yml up -d
                    """
                }
            }

            stage('Verify') {
                steps {
                    sh 'curl -f http://localhost:8081/health || exit 1'
                }
            }
        }

        post {
            success {
                notify(status: 'success', message: " ${appName}:${params.IMAGE_TAG} is LIVE")
            }
            failure {
                notify(status: 'failure', message: " Production deploy FAILED")
            }
        }
    }
}

return this