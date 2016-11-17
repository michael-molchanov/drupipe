package com.github.aroq.workflowlibs.actions

def deployFlow(params) {
    if (params.deployFlowEnvironment) {
        deployEnvironment = params.deployFlowEnvironment
    }
    else {
        deployEnvironment = environment
    }
    if (params.projectName) {
        deployProjectName = params.projectName
    }
    else {
        deployProjectName = projectName
    }

    if (deployEnvironment == params.deployFlowConfirm?.environment) {
        timeout(time: 10, unit: 'MINUTES') {
            input params.deployFlowConfirm?.message
        }
    }

    options = ''
    if (fileExists(file: params.propertiesFile)) {
        options = getOptions(readProperties(file: params.propertiesFile))
    }
    dir('druflow') {
        git 'https://github.com/aroq/druflow.git'
    }
<<<<<<< HEAD
=======
    echo "Debug flag: ${debugFlag()}"
    echo "Debug enabled: ${debugEnabled}"
>>>>>>> develop
    sh "cd druflow && ./gradlew app -Ddebug=${debugFlag()} -DprojectName=${deployProjectName} -Denv=${deployEnvironment} -DexecuteCommand=${params.executeCommand} -Dworkspace=${params.workspace} -DdocrootDir=${docrootDir}${options}"
}

def copySite(params) {
    dir('druflow') {
        git 'https://github.com/aroq/druflow.git'
    }
    sh "cd druflow && ./gradlew app -Ddebug=${debugFlag()} -Dsite=default -Denv=${params.fromEnvironment} -Dargument='${params.db} ${params.toEnvironment}' -DexecuteCommand=dbCopyAC -Dworkspace=${params.workspace} -DdocrootDir=${docrootDir}"
}

def dbBackupSite(params) {
    dir('druflow') {
        git 'https://github.com/aroq/druflow.git'
    }
    sh "cd druflow && ./gradlew app -Ddebug=${debugFlag()} -Dsite=default -Denv=${params.fromEnvironment} -Dargument=${params.db} -DexecuteCommand=dbBackupSite -Dworkspace=${params.workspace} -DdocrootDir=${docrootDir}"
}

@NonCPS
def getOptions(props) {
    result = ''
    for (prop in props) {
        result += " -D${prop.key}=${prop.value}"
    }
    result
}

def debugFlag() {
    params.debugEnabled && params.debugEnabled != '0' ? '1' : '0'
}

