def call() {
    drupipe { context ->
        drupipeBlock(nodeName: 'master', context) {
            drupipeStage('seed', context) {
                drupipeAction(
                    action: 'JobDslSeed.perform',
                    params: [
                        lookupStrategy: 'JENKINS_ROOT',
                        jobsPattern: ['library/jobdsl/job_dsl_mothership.groovy'],
                        override: true,
                        removedJobAction: 'DELETE',
                        removedViewAction: 'DELETE',
                        lookupStrategy: 'SEED_JOB',
                        additionalClasspath: ['library/src'],
                    ],
                    context
                )
            }
        }
    }
}
