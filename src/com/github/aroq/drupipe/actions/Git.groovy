package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Git extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def clone() {
        context.pipeline.script.echo 'GitArtifact retrieve'

        def repoDir = action.params.dir + '/' + action.params.repoDirName

        if (script.fileExists(repoDir)) {
            script.drupipeShell("""
            rm -fR ${repoDir}
            """, context
            )
        }

        String options = ''
        if (action.params.singleBranch) {
            options += "-b ${action.params.reference} --single-branch "
        }
        if (action.params.depth) {
            options += "--depth ${action.params.depth}"
        }
        script.drupipeShell("""
            cd ${action.params.dir}
            git clone ${options} ${action.params.repoAddress} ${action.params.repoDirName}
            """, context
        )

        context
    }
}
