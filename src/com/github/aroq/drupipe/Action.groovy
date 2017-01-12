package com.github.aroq.drupipe

class Action implements Serializable {
    String action
    String name
    String methodName
    HashMap params = [:]
    def script
    LinkedHashMap context = [:]

    String getFullName() {
        "${this.name}.${this.methodName}"
    }

    def execute() {
        def actionParams = [:]

        try {
            def utils = new com.github.aroq.drupipe.Utils()
            String drupipeStageName
            if (this.context.stage) {
                drupipeStageName = "${this.context.stage.name}"
            }
            else {
                drupipeStageName = 'config'
            }

            utils.echoDelimiter("-----> Stage: ${drupipeStageName} | Action name: ${this.fullName} start <-")
            actionParams << this.context
            if (!this.params) {
                this.params = [:]
            }
            actionParams << ['action': this]
            def defaultParams = [:]
            script.echo "Action name: ${this.name}"
            for (actionName in [this.name, this.name + '_' + this.methodName]) {
                script.echo "Checking action name: ${actionName}"
                if (actionName in params.actionParams) {
                    script.echo "ACTION DEFAULT PARAMS SET: ${params.actionParams[actionName]}"
                    defaultParams << params.actionParams[actionName]
                }
            }
            actionParams << context
            actionParams << defaultParams << this.params

            //utils.debugLog(actionParams, actionParams, "${this.fullName} action params")
            // TODO: configure it:
            def actionFile = null
            script.echo "CONTEXT sources list: ${context.sourcesList}"
            if (context.sourcesList) {
                for (def i = 0; i < context.sourcesList.size(); i++) {
                    def source = context.sourcesList[i]
                    def fileName = utils.sourcePath(context, source.name, 'pipelines/actions/' + this.name + '.groovy')
                    script.echo "FILE NAME: ${fileName}"
//                    utils.debugLog(actionParams, fileName, "Action file name to check")
                    // To make sure we only check fileExists in Heavyweight executor mode.
                    if (context.block?.nodeName && script.fileExists(fileName)) {
                        actionFile = script.load(fileName)
                        actionResult = actionFile."$action.methodName"(actionParams)
                    }
                }
            }
            def actionResult = null
            if (!actionFile) {
                try {
                    def actionInstance = this.class.classLoader.loadClass("com.github.aroq.drupipe.actions.${this.name}", true, false )?.newInstance()
                    actionResult = actionInstance."${this.methodName}"(actionParams)
                }
                catch (err) {
                    script.echo err.toString()
                    throw err
                }
            }

            if (actionResult && actionResult.returnConfig) {
                if (utils.isCollectionOrList(actionResult)) {
                    context << actionResult
                }
                else {
                    // TODO: check if this should be in else clause.
                    context << ["${action.name}.${action.methodName}": actionResult]
                }
            }
//            utils.debugLog(actionParams, context, "${this.fullName} action result")
            context.returnConfig = false
            utils.echoDelimiter "-----> Stage: ${drupipeStageName} | Action name: ${this.fullName} end <-"

            context
        }
        catch (err) {
            script.echo err.toString()
            throw err
        }
    }

}
