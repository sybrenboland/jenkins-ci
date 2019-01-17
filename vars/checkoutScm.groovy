#!/usr/bin/groovy

def call() {
    stage('Checkout SCM') {
        checkout(scm)
    }
}