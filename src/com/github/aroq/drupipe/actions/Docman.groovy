package com.github.aroq.drupipe.actions

def jsonConfig(params) {
    echo "jsonConfig params: ${params}"
    info(params)

    utils = new com.github.aroq.drupipe.Utils()
    docrootConfigJson = readFile("${params.docmanConfigPath}/${params.docmanJsonConfigFile}")
    if (env.gitlabSourceNamespace) {
       params.projectName = utils.projectNameByGroupAndRepoName(this, docrootConfigJson, env.gitlabSourceNamespace, env.gitlabSourceRepoName)
    }
    else if (projectName) {
        params.projectName = projectName
    }
    else {
        // TODO: refactor it
        params.projectName = 'common'
    }
    echo "PROJECT NAME: ${params.projectName}"

    params << [returnConfig: true]
}

def info(params) {
    if (params.force == '1') {
        echo "Force mode"
        drupipeShell(
            """
            if [ "${params.force}" == "1" ]; then
              rm -fR ${params.docrootDir}
            fi
            """, params << [shellCommandWithBashLogin: true]
        )
    }

    configRepo = false
    try {
        if (config_repo) {
            configRepo = config_repo
        }
    }
    catch (err) {

    }

    echo "Config repo: ${configRepo}"

    if (configRepo && !fileExists('docroot')) {
        echo 'Docman init'
        drupipeShell(
            """
            docman init ${params.docrootDir} ${configRepo} -s
            """, params << [shellCommandWithBashLogin: true]
        )
    }
    echo 'Docman info'
    drupipeShell(
        """
        cd ${params.docrootDir}
        docman info full config.json
        """, params << [shellCommandWithBashLogin: true]
    )
}

def build(params) {
    jsonConfig(params)
    deploy(params)
    params << [returnConfig: true]
    params
}

def deploy(params) {
    echo "FORCE MODE: ${params.force}"
    def flag = ''
    if (params.force == '1') {
        flag = '-f'
    }

    if (params.projectName) {
        deployProjectName = params.projectName
    }
    else {
        deployProjectName = projectName
    }

    echo "docman deploy git_target ${deployProjectName} branch ${version} ${flag}"

    drupipeShell(
        """
        if [ "${params.force}" == "1" ]; then
          rm -fR ${params.docrootDir}
        fi
        docman init ${params.docrootDir} ${config_repo} -s
        cd docroot
        docman deploy git_target ${deployProjectName} branch ${version} ${flag}
        """, params << [shellCommandWithBashLogin: true]
    )
}

def init(params) {
    if (params.configRepo) {
        configRepo = params.configRepo
    }
    if (config_repo) {
        configRepo = config_repo
    }
    if (configRepo) {
        drupipeShell(
            """
            docman init ${params.path} ${configRepo} -s
            """, params << [shellCommandWithBashLogin: true]
        )
        params.dir
    }
    else {
        null
    }
}

