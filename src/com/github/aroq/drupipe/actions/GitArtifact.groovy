package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class GitArtifact extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def retrieve() {
        script.drupipeAction([action: "Git.clone", params: context.builder.artifactParams], context)
    }
}
