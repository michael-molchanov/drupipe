package com.github.aroq.workflowlibs.actions

def add(params) {
    def source = params.source
    def result
    utils = new com.github.aroq.workflowlibs.Utils()
    dump(params, "source start")
    switch (source.type) {
        case 'git':
            dir(source.path) {
                git url: source.url, branch: source.branch
            }
            result = source.path
            break

        case 'dir':
            result = source.path
            break

        case 'docmanDocroot':
            result = executeAction(utils.processPipelineAction([action: 'Docman.init', params: [path: 'docroot']])) {
                p = params
            }
            break
    }
    if (!params.sources) {
        params.sources = [:]
    }
    if (result) {
        params.sources[source.name] = source
    }
    params.remove('source')
    dump(params, "source end")
    params
}

def loadConfig(params) {
    configFilePath = sourcePath(params, params.sourceName, params.configPath)
    utils = new com.github.aroq.workflowlibs.Utils()

    if (params.configType == 'groovy') {
        params << executeAction(utils.processPipelineAction([action: 'GroovyFileConfig.load', params: [configFileName: configFilePath]])) {
            p = params
        }
    }
    params.remove('sourceName')
    params.remove('configPath')
    params.remove('configType')
    params
}
