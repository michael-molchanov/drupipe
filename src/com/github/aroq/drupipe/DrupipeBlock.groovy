package com.github.aroq.drupipe

class DrupipeBlock implements Serializable {

    ArrayList<DrupipeStage> stages = []

    String nodeName = 'use_default'

    Boolean withDocker = false

    String dockerImage = 'use_default'

    LinkedHashMap context = [:]

    def execute(c, body = null) {
        if (c) {
            this.context = c
        }

        if (nodeName == 'use_default') {
            nodeName = context.nodeName
        }

        if (dockerImage == 'use_default') {
            dockerImage = context.dockerImage
        }
        context.dockerImage = dockerImage

        def result = [:]
        context.block = this

        if (nodeName) {
            context.pipeline.script.node(nodeName) {
                if (withDocker) {
                    context.pipeline.script.drupipeWithDocker(context) {
                        result = _execute(body)
                    }
                }
                else {
                    result = _execute(body)
                }
            }
        }
        else {
            result = _execute(body)
        }

        result
    }

    def _execute(body = null) {
        if (stages) {
            context << context.pipeline.executeStages(stages, context)
        }
        else {
            context << body()
        }
        context
    }
}