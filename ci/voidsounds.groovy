def call(Map config = [:]) {
    def appName = config.appName ?: 'Clemenineqq/voidsounds'
    def registry = config.registry ?: 'ghcr.io'

    pipeline {
        agent { label 'docker' }

        environment {
            IMAGE_TAG = "${env.BUILD_NUMBER}"
        }

        stages {
            stage('Install & Test') {
                steps {
                    runTests(action: 'all')
                }
            }

            stage('Build Image') {
                steps {
                    buildImage(
                        imageName: appName,
                        imageTag: env.IMAGE_TAG
                    )
                }
            }

            stage('Push to Registry') {
                steps {
                    pushImage(
                        registry: registry,
                        imageName: appName,
                        imageTag: env.IMAGE_TAG,
                        credentialsId: 'github-registry'
                    )
                }
            }
        }

        post {
            success {
                notify(status: 'success', message: "Built ${appName}:${env.IMAGE_TAG}")
            }
            failure {
                notify(status: 'failure', message: "CI failed for ${appName}")
            }
        }
    }
}

return this