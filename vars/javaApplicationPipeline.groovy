#!/usr/bin/groovy

def call(config = [:]) {
    config = config as JavaBuildConfig

    podTemplate(
            label: 'slave-pod',
            inheritFrom: 'default',
            containers: [
                    containerTemplate(name: 'maven', image: config.mavenImage, ttyEnabled: true, command: 'cat'),
                    containerTemplate(name: 'docker', image: 'docker:18.02', ttyEnabled: true, command: 'cat'),
                    containerTemplate(name: 'kubectl', image: 'traherom/kustomize-docker:1.0.5', ttyEnabled: true, command: 'cat')
            ],
            volumes: [
                    hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
                    hostPathVolume(hostPath: '/root/.m2', mountPath: '/root/.m2')
            ]
    ) {
        node('slave-pod') {
            try {
                def repoName
                def commitId
                stage ('Extract') {
                    checkout scm
                    repoName = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/')[3].split("\\.")[0]
                    commitId = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                }

                stage ('Build') {
                    container ('maven') {
                        sh 'mvn clean install'
                    }
                }

                def imageTag
                stage ('Docker build and push') {
                    container ('docker') {
                        withCredentials([usernamePassword(credentialsId: 'dockerhub',
                                usernameVariable: 'registryUser', passwordVariable: 'registryPassword')]) {

                            imageTag = "$registryUser/$repoName:$commitId"
                            sh "docker login -u=$registryUser -p=$registryPassword"
                            sh "docker build -t $imageTag ."
                            sh "docker push $imageTag"
                        }
                    }
                }

                stage ("Deploy") {
                    container ('kubectl') {
                        dir ("deployment") {
                            sh """
                                kustomize edit set imagetag $imageTag;
                                kustomize build overlays/test | kubectl apply --record -f  -
                            """
                        }
                    }
                }
            } finally {
                cleanWs()
            }
        }
    }
}