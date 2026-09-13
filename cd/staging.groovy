def call(Map config = [:]) {
    def appName = config.appName ?: 'voidsounds'
    def registry = config.registry ?: 'ghcr.io'

    pipeline {
        agent { label 'deploy' }

        parameters {
            string(name: 'IMAGE_TAG', description: 'Which image tag to deploy')
        }

        stages {
            stage('Deploy') {
                steps {
                    sh """
                        docker pull ${registry}/${appName}:${params.IMAGE_TAG}
                        docker-compose -f docker-compose.staging.yml up -d
                    """
                }
            }

            stage('Health Check') {
                steps {
                    sh 'curl -f http://localhost:8080/health || exit 1'
                }
            }
        }

        post {
            success {
                notify(status: 'success', message: "Deployed ${appName}:${params.IMAGE_TAG} to staging")
            }
            failure {
                notify(status: 'failure', message: "Deploy FAILED: ${appName}:${params.IMAGE_TAG}")
            }
        }
    }
}

return this