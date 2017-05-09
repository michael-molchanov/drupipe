import com.github.aroq.GitlabHelper

println "Subjobs Job DSL processing"

def config = ConfigSlurper.newInstance().parse(readFileFromWorkspace('config.dump.groovy'))

def gitlabHelper = new GitlabHelper(script: this, config: config)

if (config.jobs) {
    def pipelineScript = config.pipeline_script ? config.pipeline_script : 'pipeline'

    def repo = config.defaultActionParams.SeleneseTester.repoAddress
    def branch = config.defaultActionParams.SeleneseTester.reference ? config.defaultActionParams.SeleneseTester.reference : 'master'

    if (config.env.GITLAB_API_TOKEN_TEXT) {
        users = gitlabHelper.getUsers(repo)
        println "USERS: ${users}"
    }

    processJob(config.jobs, '', users, repo, branch, config)

}

def processJob(jobs, currentFolder, users, repo, b, config) {
    jobs.each { job ->
        println "Processing job: ${job.name}"
        def currentName = currentFolder ? "${currentFolder}/${job.name}" : job.name
        println "Type: ${job.type}"
        println "Current name: ${currentName}"
        if (job.type == 'folder') {
            folder(currentName) {
                authorization {
                    users.each { user ->
                        // TODO: make permissions configurable.
                        if (user.value > 10) {
                            permission('hudson.model.Item.Read', user.key)
                        }
                        if (user.value > 30) {
                            permission('hudson.model.Run.Update', user.key)
                            permission('hudson.model.Item.Build', user.key)
                            permission('hudson.model.Item.Cancel', user.key)
                        }
                    }
                }
            }
            currentFolder = currentName
        }
        else {
            if (job.pipeline && job.pipeline.repo == 'config') {
                repo = config.configRepo
            }
            if (job.type == 'selenese') {
                pipelineJob("${currentName}") {
                    concurrentBuild(false)
                    logRotator(-1, 30)
                    parameters {
                        stringParam('debugEnabled', '0')
                    }
                    definition {
                        cpsScm {
                            scm {
                                git() {
                                    remote {
                                        name('origin')
                                        url(repo)
                                        credentials(config.credentialsId)
                                    }
                                    branch(b)
                                }
                                scriptPath(job.pipeline.file)
                            }
                        }
                    }
                }
            }
        }

        if (job.children) {
            processJob(job.children, currentFolder, users, repo, b, config)
        }
    }
}

Map merge(Map[] sources) {
    if (sources.length == 0) return [:]
    if (sources.length == 1) return sources[0]

    sources.inject([:]) { result, source ->
        source.each { k, v ->
            result[k] = result[k] instanceof Map && v instanceof Map ? merge(result[k], v) : v
        }
        result
    }
}