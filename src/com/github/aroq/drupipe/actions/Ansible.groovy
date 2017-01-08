package com.github.aroq.drupipe.actions

def deployWithGit(params) {
    utils = new com.github.aroq.drupipe.Utils()
    utils.loadLibrary(this, params)
    // TODO: Provide Ansible parameters automatically when possible (e.g. from Docman).
    // params.ansible << [:]
    executeAnsiblePlaybook(params)
}

def deployWithAnsistrano(params) {
    // TODO: refactor it.
    sh("ansible-galaxy install carlosbuenosvinos.ansistrano-deploy carlosbuenosvinos.ansistrano-rollback")
    utils = new com.github.aroq.drupipe.Utils()
    utils.loadLibrary(this, params)
    // TODO: Provide Ansible parameters automatically when possible (e.g. from Docman).
    def version = readFile('docroot/master/VERSION')
    params << [ansible_reference: version]
    executeAnsiblePlaybook(params)
}

def executeAnsiblePlaybook(params, environmentVariables = [:]) {
    def command =
        "ansible-playbook ${params.ansible_playbook} \
        -i ${params.ansible_hostsFile} \
        -e 'target=${params.ansible_target} \
        user=${params.ansible_user} \
        repo=${params.ansible_repo} \
        reference=${params.ansible_reference} \
        deploy_to=${params.ansible_deploy_to}'"

    echo "Ansible command: ${command}"

    wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
        sh"""#!/bin/bash -l
            ${command}
        """
    }
}

