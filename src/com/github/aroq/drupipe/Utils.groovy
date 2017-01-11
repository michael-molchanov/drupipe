package com.github.aroq.drupipe

import groovy.json.JsonSlurper
import com.github.aroq.drupipe.Action
import com.github.aroq.drupipe.Stage
import groovy.json.JsonOutput

def colorEcho(message, color = null) {
    if (!color) {
        color = 'green'
    }
    switch (color) {
        case 'red':
            color = 31
            break
        case 'green':
            color = 32
            break
        case 'yellow':
            color = 33
            break
        case 'blue':
            color = 34
            break
        case 'magenta':
            color = 35
            break
        case 'cyan':
            color = 36
            break
    }

    wrap([$class: 'AnsiColorBuildWrapper']) {
        echo "\u001B[${color}m${message}\u001B[0m"
    }
}

@NonCPS
List<Stage> processStages(stages) {
    List<Stage> result = []
    for (item in stages) {
        result << processStage(item)
    }
    result
}

@NonCPS
Stage processStage(stage) {
    if (stage instanceof Stage) {
        for (action in stage.actions) {
            values = action.action.split("\\.")
            action.name = values[0]
            action.methodName = values[1]
        }
        stage
    }
    else {
        new Stage(name: stage.key, actions: processPipelineActionList(stage.value))
    }
}

@NonCPS
List processPipelineActionList(actionList) {
    List actions = []
    for (action in actionList) {
        actions << processPipelineAction(action)
    }
    actions
}

@NonCPS
Action processPipelineAction(action) {
    if (action.getClass() == java.lang.String) {
        actionName = action
        actionParams = [:]
    }
    else {
        actionName = action.action
        actionParams = action.params
    }
    values = actionName.split("\\.")
    new Action(name: values[0], methodName: values[1], params: actionParams)
}

@NonCPS
def projectNameByGroupAndRepoName(script, docrootConfigJson, groupName, repoName) {
    // TODO: Refactor it.
    groupName = groupName.toLowerCase()
    repoName = repoName.toLowerCase()
    docmanConfig = JsonSlurper.newInstance().parseText(docrootConfigJson)
    result = ''
    docmanConfig.projects.each { project ->
        def repo = project.value['repo'];
        if (repo) {
        script.echo "REPO: ${repo.toLowerCase()}"
        script.echo "GITLAB: ${groupName}/${repoName}"
            if (repo.toLowerCase().contains("${groupName}/${repoName}")) {
                result = project.value['name']
            }
        }
    }
    result
}

def writeEnvFile() {
    sh 'env > env.txt'
    writeFile(file: 'ENV.groovy', text: envConfig(readFile('env.txt')))
    sh 'rm -fR env.txt'
}

def envToMap() {
    sh 'env > env.txt'
    def result = envTextToMap(readFile('env.txt'))
    sh 'rm -fR env.txt'
    result
}

def dumpConfigFile(config, fileName = 'config.dump.groovy') {
    echo "Dumping config file: config.dump.groovy"
    writeFile(file: fileName, text: configToSlurperFile(config))
    sh "cat ${fileName}"
}

@NonCPS
String envConfig(env) {
    def co = new ConfigObject()
    env.split("\r?\n").each {
        co.put(it.substring(0, it.indexOf('=')), it.substring(it.indexOf('=') + 1))
    }
    def sw = new StringWriter()
    co.writeTo(sw)
    sw.toString()
}

@NonCPS
def envTextToMap(env) {
    def result = [:]
    env.split("\r?\n").each {
        result.put(it.substring(0, it.indexOf('=')), it.substring(it.indexOf('=') + 1))
    }
    result
}

@NonCPS
String configToSlurperFile(config) {
    def co = new ConfigObject()
    skipConfigKeys = ['action', 'sources', 'sourcesList', 'stage']
    config.each { entry ->
        if (!skipConfigKeys.contains(entry.key)) {
            co.put(entry.key, entry.value)
        }
    }
    def sw = new StringWriter()
    co.writeTo(sw)
    sw.toString()
}

String getJenkinsFolderName(String buildUrl) {
    (buildUrl =~ $/job/(.+)/job/(.+)/.*/$)[ 0 ] [ 1 ]
}

String getJenkinsJobName(String buildUrl) {
    (buildUrl =~ $/job/(.+)/job/(.+)/.*/$)[ 0 ] [ 2 ]
}

@NonCPS
def getMothershipProjectParams(config, json) {
    def projects = JsonSlurper.newInstance().parseText(json).projects
    echo "MOTHERSHIP PROJECTS: ${projects}"
    projects[config.jenkinsFolderName] ? projects[config.jenkinsFolderName] : [:]
}

def loadLibrary(script, params) {
    script.drupipeAction([
        action: 'Source.add',
        params: [
            source: [
                name: 'library',
                type: 'git',
                path: 'library',
                url: params.drupipeLibraryUrl,
                branch: params.drupipeLibraryBranch,
                refType: params.drupipeLibraryType,
            ],
        ],
    ], params)
}

def executeAction(action, params) {
    def actionParams = [:]

    try {
        String drupipeStagename
        if (params.stage) {
            drupipeStageName = "${params.stage.name}"
        }
        else {
            drupipeStageName = 'config'
        }

        echoDelimiter("-----> Stage: ${drupipeStageName} | Action name: ${action.fullName} start <-")
        actionParams << params
        if (!action.params) {
            action.params = [:]
        }
        actionParams << ['action': action]
        defaultParams = [:]
        for (actionName in [action.name, action.name + '_' + action.methodName]) {
            if (actionName in params.actionParams) {
                defaultParams << params.actionParams[actionName]
            }
        }
        actionParams << params
        actionParams << defaultParams << action.params

        debugLog(actionParams, actionParams, "${action.fullName} action params")
        // TODO: configure it:
        def actionFile = null
        if (params.sourcesList) {
            for (i = 0; i < params.sourcesList.size(); i++) {
                source = params.sourcesList[i]
                fileName = sourcePath(params, source.name, 'pipelines/actions/' + action.name + '.groovy')
                debugLog(actionParams, fileName, "Action file name to check")
                // To make sure we only check fileExists in Heavyweight executor mode.
                if (params.block?.nodeName && fileExists(fileName)) {
                    actionFile = load(fileName)
                    actionResult = actionFile."$action.methodName"(actionParams)
                }
            }
        }
        if (!actionFile) {
            try {
                def actionInstance = this.class.classLoader.loadClass("com.github.aroq.drupipe.actions.${action.name}", true, false )?.newInstance()
                actionResult = actionInstance."$action.methodName"(actionParams)
            }
            catch (err) {
                echo err.toString()
                throw err
            }
        }

        if (actionResult && actionResult.returnConfig) {
            if (isCollectionOrList(actionResult)) {
                params << actionResult
            }
            else {
                // TODO: check if this should be in else clause.
                params << ["${action.name}.${action.methodName}": actionResult]
            }
        }
        debugLog(actionParams, params, "${action.fullName} action result")
        params.returnConfig = false
        echoDelimiter "-----> Stage: ${drupipeStageName} | Action name: ${action.fullName} end <-"

        params
    }
    catch (err) {
        echo err.toString()
        throw err
    }
}

boolean isCollectionOrList(object) {
    object instanceof java.util.Collection || object instanceof java.util.List || object instanceof java.util.LinkedHashMap || object instanceof java.util.HashMap
}

def executePipelineActionList(actions, params) {
    actionList = processPipelineActionList(actions)
    debugLog(params, actionList, 'action list', [debugMode: 'json'])
    params << executeActionList(actionList, params)
    params
}

def executeActionList(actionList, params) {
    try {
        for (action in actionList) {
            params << executeAction(action, params)
        }
        params
    }
    catch (err) {
        echo err.toString()
        throw err
    }

    params
}

def _executeStage(name, config, body) {
    stage(name) {
        gitlabCommitStatus(name) {
            config << body()
        }
    }
    config
}

def executeStage(Stage stageInstance, params) {
    _executeStage(stageInstance.name, params) {
        params << ['stage': stageInstance]
        params << executeActionList(stageInstance.actions, params)
    }
}

def executeStages(stagesToExecute, params) {
    stages = processStages(stagesToExecute)
    stages += processStages(params.stages)

    for (int i = 0; i < stages.size(); i++) {
        params << executeStage(stages[i], params)
    }
    params
}

def pipelineNotify(params, String buildStatus = 'STARTED') {
    // build status of null means successful
    buildStatus =  buildStatus ?: 'SUCCESSFUL'

    // Default values
    def colorName = 'RED'
    def colorCode = '#FF0000'
    def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
    def summary = "${subject} (${env.BUILD_URL})"
    def details = """<p>Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at <a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a></p>"""

    // Override default values based on build status
    if (buildStatus == 'STARTED') {
        color = 'YELLOW'
        colorCode = '#FFFF00'
    } else if (buildStatus == 'SUCCESSFUL') {
        color = 'GREEN'
        colorCode = '#00FF00'
    } else {
        color = 'RED'
        colorCode = '#FF0000'
    }

    // Send notifications
    if (params.notificationsSlack) {
        try {
            slackSend (color: colorCode, message: summary, channel: params.slackChannel)
        }
        catch (e) {
            echo 'Unable to sent Slack notification'
        }
    }

    if (params.notificationsMattermost) {
        try {
            mattermostSend (color: colorCode, message: summary, channel: params.mattermostChannel)
        }
        catch (e) {
            echo 'Unable to sent Mattermost notification'
        }
    }

    // hipchatSend (color: color, notify: true, message: summary)

    if (params.notificationsEmailExt) {
        def to = emailextrecipients([
            [$class: 'CulpritsRecipientProvider'],
            [$class: 'DevelopersRecipientProvider'],
            [$class: 'RequesterRecipientProvider']
        ])

        emailext (
            subject: subject,
            body: details,
            to: to,
            mimeType: 'text/html',
            attachLog: true,
        )
    }
}

def sourcePath(params, sourceName, String path) {
    debugLog(params, sourceName, 'Source name')
    if (sourceName in params.sources) {
        params.sources[sourceName].path + '/' + path
    }
}


def debugLog(params, value, dumpName = '', debugParams = [:]) {
    if (params.debugEnabled) {
        if (value instanceof java.lang.String) {
            echo "${dumpName}: ${value}"
        }
        else {
            if (debugParams?.debugMode == 'json' || params.debugMode == 'json') {
                jsonDump(value, dumpName)
            }
            else {
                dump(value, dumpName)
            }
        }
    }
}

def dump(params, String dumpName = '') {
    colorEcho "Dumping ${dumpName}:"
    colorEcho collectParams(params)
}

@NonCPS
def collectParams(params) {
    def String result = ''
    for (item in params) {
        result = result + "${item.key} = ${item.value}\r\n"
    }
    result
}

def echoDelimiter(String message) {
    if (message) {
        if (message.size() < 80) {
            echo message + '-' * (80 - message.size())
        }
        else {
            echo message
        }
    }
}

def jsonDump(value, String dumpName = '') {
    if (dumpName) {
        echo dumpName
    }
    echo JsonOutput.prettyPrint(JsonOutput.toJson(value))
}

return this
