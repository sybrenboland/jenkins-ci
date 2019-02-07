#!/usr/bin/groovy

def call(config = [:]) {
    config = config as NpmLibraryPipelineConfig

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

                def isRelease = env.BRANCH_NAME == config.releaseBranch
                echo "Is Release ????"
                echo env.BRANCH_NAME
                echo config.releaseBranch

                if (isRelease) {
                    def releaseType
                    stage('Initialize Release') {
                        releaseType = input(id: 'releaseTypeInput', message: 'Please specify release type', parameters: [
                                [$class: 'ChoiceParameterDefinition', choices: 'patch\nminor\nmajor', description: 'releaseType', name: 'releaseType']
                        ])
                    }

                    stage('Bump Version') {
                        container ('node') {
                            withCredentials([usernamePassword(credentialsId: 'github', usernameVariable: 'gitUser', passwordVariable: 'gitPassword')]) {
                                sh """
                                      git config --global --replace-all credential.helper \'/bin/bash -c \"echo username=$gitUser; echo password=$gitPassword\"\'
                                      git config --global user.name "$gitUser"
                                      git config --global user.email "$gitUser@gmail.com"
                                   """

                                sh(script: "npm version ${releaseType} -m \"release: version %s\"", returnStdout: true)
                            }
                        }
                    }

                    buildNpm()

                    stage('Publish Release') {
                        container ('node') {
                            withCredentials([usernamePassword(credentialsId: 'github', usernameVariable: 'gitUser', passwordVariable: 'gitPassword')]) {
                                sh """
                                      git config --global --replace-all credential.helper \'/bin/bash -c \"echo username=$gitUser; echo password=$gitPassword\"\'
                                      git config --global user.name "$gitUser"
                                      git config --global user.email "$gitUser@gmail.com"
                                   """

                                sh "npm run publish"

                                sh """
                                       git push --atomic origin \
                                           HEAD:master \
                                           refs/tags/$releaseVersion
                                   """
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