package com.github.aroq.drupipe.actions

class Commands extends BaseAction {

    def execute() {
        def commands = []
        if (!action.params.containsKey('commands')) {
            action.pipeline.drupipeLogger.error "Commands are not defined"
        }

        if (action.params.aggregate_commands) {
            commands.add("cd ${action.params.execution_dir}" + ' && ' + action.params.commands.join(' && '))
            action.pipeline.drupipeLogger.error "Commands are not defined"
        }
        else {
            commands = action.params.commands.collect("cd {$action.params.execution_dir}" + ' && ' + it)
        }

        def prepareSSHChainCommand = { String command, int level ->
            return "${command}-${level}"
        }

        for (command in commands) {
            action.pipeline.drupipeLogger.info "Execute command: ${command}"
            if (action.params.containsKey('through_ssh_chain')) {
                int level = 0
                String chainCommand = command
                for (String sshChainItem in action.params.through_ssh_chain) {
                    level++
                    chainCommand = "${action.params.through_ssh_chain.executable} ${action.params.through_ssh_chain.options} ${sshChainItem} ${prepareSSHChainCommand(chainCommand, level)}"
                }
            }
        }
    }
}
