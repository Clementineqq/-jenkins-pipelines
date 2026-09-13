def call(Map config = [:]) {
    def appName = config.appName ?: 'voidsounds'
    def registry = config.registry ?: 'registry.example.com'

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
                        credentialsId: 'docker-registry'
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