def call(params = [:], body) {

    if (params.p) {
        params << params.p
        params.remove('p')
    }

    node(params.nodeName) {
//        withCredentials([[$class: 'FileBinding', credentialsId: 'id_rsa', variable: 'ID_RSA_FILE']]) {
            def drudock = docker.image('aroq/drudock:1.0.1')
            drudock.pull()
            drudock.inside('--user root:root') {
                echo "Credentials: ${params.credentialsID}"
                sshagent([params.credentialsID]) {
                    'echo ssh'
                    sh "ssh -v git@code.adyax.com"
                    checkout scm
                    if (params.pipeline) {
                        params = executePipeline {
                            noNode = true
                            credentialsID = params.credentialsID
                            pipeline = params.pipeline
                        }
                    }
                    body()
                }
            }
        }
//    }

    params
}
