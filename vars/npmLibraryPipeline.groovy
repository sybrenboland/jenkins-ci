#!/usr/bin/groovy

def call(config = [:]) {
    config = config as NpmLibraryPipelineConfig
    GitUtils gitUtils = new GitUtils()

    podTemplate(
            label: 'slave-pod',
            inheritFrom: 'default',
            containers: [containerTemplate(name: 'node', image: config.nodeImage, ttyEnabled: true, command: 'cat')]
    ) {
        node('slave-pod') {
            try {
                stage ('Extract') {
                    checkout scm
                }

                if (env.BRANCH_NAME == config.releaseBranch) {
                    def releaseType
                    stage('Initialize Release') {
                        releaseType = input(id: 'releaseTypeInput', message: 'Please specify release type', parameters: [
                                [$class: 'ChoiceParameterDefinition', choices: 'patch\nminor\nmajor', description: 'releaseType', name: 'releaseType']
                        ])
                    }

                    def releaseVersion
                    stage('Bump Version') {
                        container ('node') {
                            gitUtils.withGitCredentials {
                                releaseVersion = sh(script: "npm version ${releaseType} -m \"release: version %s\"", returnStdout: true)
                            }
                        }
                    }

                    buildNpm()

                    stage('Publish Release') {
                        container ('node') {
                            gitUtils.withGitCredentials {
                                // sh 'npm run publish'

                                sh "git push --atomic origin HEAD:master refs/tags/$releaseVersion"
                            }
                        }
                    }
                } else {
                    buildNpm()
                }
            } finally {
                cleanWs()
            }
        }
    }
}