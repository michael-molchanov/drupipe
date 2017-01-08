package com.github.aroq.drupipe.actions

def projectMembers(params) {
    withCredentials([string(credentialsId: 'gitlab_api_token_text', variable: 'gitlab_api_token')]) {
        echo "Gitlab API Token: ${gitlab_api_token}"
    }
}