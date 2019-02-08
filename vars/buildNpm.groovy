#!/usr/bin/groovy

def call() {
    stage('Build Npm') {
        container ('node') {
            sh(script: "node -pe \"require('./package.json').version\"", returnStdout: true)
            sh "npm install"
            sh "npm run lint"
            sh "npm run build"
            sh "npm run build.demo"
        }
    }
}