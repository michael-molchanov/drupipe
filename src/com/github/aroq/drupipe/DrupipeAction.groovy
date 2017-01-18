package com.github.aroq.drupipe

class DrupipeAction implements Serializable {

    String action

    String name

    String methodName

    HashMap params = [:]

    LinkedHashMap context = [:]

    String getFullName() {
        "${this.name}.${this.methodName}"
    }

    def execute(c = null) {
        if (c) {
            this.context << c
        }
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

            utils.echoDelimiter("-----> DrupipeStage: ${drupipeStageName} | DrupipeAction name: ${this.fullName} start <-")
            actionParams << this.context
            if (!this.params) {
                this.params = [:]
            }
            actionParams << ['action': this]
            def defaultParams = [:]
            for (actionName in [this.name, this.name + '_' + this.methodName]) {
                if (actionName in context.actionParams) {
                    defaultParams << context.actionParams[actionName]
                }
            }
            actionParams << context
            actionParams << defaultParams << this.params

            utils.debugLog(actionParams, actionParams, "${this.fullName} action params")
            // TODO: configure it:
            def actionFile = null
            def actionResult = null
            if (context.sourcesList) {
                for (def i = 0; i < context.sourcesList.size(); i++) {
                    def source = context.sourcesList[i]
                    def fileName = utils.sourcePath(context, source.name, 'pipelines/actions/' + this.name + '.groovy')
                    utils.debugLog(actionParams, fileName, "DrupipeAction file name to check")
                    // To make sure we only check fileExists in Heavyweight executor mode.
                    if (context.block?.nodeName && this.context.pipeline.script.fileExists(fileName)) {
                        actionFile = this.context.pipeline.script.load(fileName)
                        actionResult = actionFile."${this.methodName}"(actionParams)
                    }
                }
            }
            if (!actionFile) {
                try {
                    def actionInstance = this.class.classLoader.loadClass("com.github.aroq.drupipe.actions.${this.name}", true, false )?.newInstance()
                    actionResult = actionInstance."${this.methodName}"(actionParams)
                }
                catch (err) {
                    this.context.pipeline.script.echo err.toString()
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
            utils.debugLog(actionParams, context, "${this.fullName} action result")
            context.returnConfig = false
            utils.echoDelimiter "-----> DrupipeStage: ${drupipeStageName} | DrupipeAction name: ${this.fullName} end <-"

            context
        }
        catch (err) {
            this.context.pipeline.script.echo err.toString()
            throw err
        }
    }

}