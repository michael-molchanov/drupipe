package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper

class YamlFileHandler extends BaseAction {

    def context

    def script

    def utils

    def deployYaml

    def DrupipeActionWrapper action

    def init() {
        if (!action.pipeline.context.tags.contains('docman')) {
            def repo_url
            if (action.pipeline.context.components && action.pipeline.context.components['master'] && action.pipeline.context.components['master'].repo) {
                repo_url = action.pipeline.context.components['master'].repo
            }
            else {
                repo_url = action.pipeline.context.configRepo
            }

            def branch = 'master'
            if (action.pipeline.context.environmentParams.git_reference) {
                branch = action.pipeline.context.environmentParams.git_reference
            }
            else {
                branch = action.pipeline.context.job.branch
            }

            def repoParams = [
                repoAddress: repo_url,
                reference: branch,
                dir: 'docroot',
                repoDirName: 'master',
            ]
            script.drupipeAction([action: "Git.clone", params: repoParams << action.params], action.pipeline)
        }
    }

    def findDeployYaml() {
        def file
        def files
        files = script.findFiles(glob: "**/${action.pipeline.context.projectConfigPath}/.unipipe/${action.params.deployFile}")
        if (files.size() > 0) {
            script.echo files[0].path
            return files[0].path
        }

        files = script.findFiles(glob: "**/${action.pipeline.context.projectConfigPath}/.drupipe/${action.params.deployFile}")
        if (files.size() > 0) {
            script.echo files[0].path
            return files[0].path
        }

        files = script.findFiles(glob: "**/${action.pipeline.context.projectConfigPath}/${action.params.deployFile}")
        if (files.size() > 0) {
            script.echo files[0].path
            return files[0].path
        }
        return false
    }

    def build() {
        process('build')
    }

    def deploy() {
        process('deploy')
    }

    def operations() {
        process('operations')
    }

    def test() {
        process('test')
    }

    def process(String stage) {
        init()
        String deployDir = 'docroot/master'
        action.pipeline.context['builder']['artifactParams'] = [:]
        action.pipeline.context['builder']['artifactParams']['dir'] = deployDir
        action.pipeline.context.builder['buildDir'] = "${action.pipeline.context.docrootDir}/master"
        def deployYamlFile = findDeployYaml()
        if (deployYamlFile && script.fileExists(deployYamlFile)) {
            def deployYAML = script.readYaml(file: deployYamlFile)
            utils.dump(action.pipeline.context, deployYAML, 'DEPLOY YAML')
            def commands = []
            if (stage == 'operations') {
                def root = action.pipeline.context.environmentParams.root
                root = root.substring(0, root.length() - (root.endsWith("/") ? 1 : 0))
                commands << "cd ${root}"
                commands << "pwd"
                commands << "ls -lah"
            }
            else {
                commands << "cd ${deployDir}"
                commands << "pwd"
                commands << "ls -lah"
            }
            if (deployYAML[stage]) {
                for (def i = 0; i < deployYAML[stage].size(); i++) {
                    commands << interpolateCommand(deployYAML[stage][i])
                }
            }
            else {
                script.echo "No ${stage} defined in ${action.params.deployFile}"
            }
            if (commands) {
                def joinedCommands = commands.join("\n")
                if (stage == 'operations') {
                    executeCommand(joinedCommands)
                }
                else {
                    script.drupipeShell(joinedCommands, action.params)
                }
            }
        }
        else {
            script.echo "Deploy file ${action.params.deployFile} doesn't exist"
        }
    }

    @NonCPS
    def interpolateCommand(String command) {
        def binding = [context: action.pipeline.context, action: action]
        def engine = new groovy.text.SimpleTemplateEngine()
        def template = engine.createTemplate(command).make(binding)
        template.toString()
    }

    def executeCommand(String command) {
        script.drupipeShell(
            """
            ssh ${action.pipeline.context.environmentParams.user}@${action.pipeline.context.environmentParams.host} "${command}"
            """, action.params
        )
    }
}
