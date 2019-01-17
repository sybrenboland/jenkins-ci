#!/usr/bin/groovy

def call(config = [:]) {
    node('docker') {
        try {
            config = config as JavaBuildConfig

            checkoutScm()

            buildMaven(config.mavenImage)

            currentBuild.displayName = readMavenPom().getVersion()

        } finally {
            cleanWs()
        }
    }
}