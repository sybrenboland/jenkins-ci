def withGitCredentials(Closure closure) {
    closure.setDelegate(this)
    withCredentials([usernamePassword(credentialsId: 'github', usernameVariable: 'gitUser', passwordVariable: 'gitPassword')]) {
        sh """
              git config --global --replace-all credential.helper \'/bin/bash -c \"echo username=$gitUser; echo password=$gitPassword\"\'
              git config --global user.name "$gitUser"
              git config --global user.email "$gitUser@gmail.com"
           """
        closure.call()
    }
}

return this