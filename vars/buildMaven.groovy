#!/usr/bin/groovy

def call(String mavenImage) {
    stage('Build Maven') {

        docker.image(mavenImage).inside('-v /root/.m2:/root/.m2') {

            def mavenGoals = ["clean", "install"]

            def mavenGoalsString = mavenGoals.join(" ")

            sh "mvn $mavenGoalsString"
        }
    }
}