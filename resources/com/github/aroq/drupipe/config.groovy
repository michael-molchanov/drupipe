debugEnabled = false
docrootDir = 'docroot'
docmanConfigPath = 'docroot/config'
docmanConfigFile = 'docroot.config'
docmanJsonConfigFile = 'config.json'

drupipeLibraryUrl = 'https://github.com/aroq/drupipe.git'
drupipeLibraryBranch = 'master'
drupipeLibraryType = 'branch'
drupipeDocker = true
nodeName = 'default'

// Environments section.
environments {
    dev {
        drupipeLibraryBranch = 'develop'
    }
    test {
        drupipeLibraryBranch = 'master'
    }
    prod {
        drupipeLibraryBranch = 'v0.2.2'
        drupipeLibraryType = 'tag'
    }
}

actionParams = [
    // TODO: add params subsections (that will be containerized inside common config).
    Config: [
        projectConfigPath: 'docroot/config',
        projectConfigFile: 'docroot.config',
        mothershipConfigFile: 'mothership.config',
    ],
    Behat: [
        masterPath: 'docroot/master',
        masterRelativePath: '..',
        behatExecutable: 'bin/behat',
        pathToEnvironmentConfig: 'code/common',
        workspaceRelativePath: '../../..',
        behat_args: '--format=pretty --out=std --format=junit',
    ],
    withDrupipeDocker: [
        drupipeDockerImageName: 'aroq/drudock:1.2.0',
        drupipeDockerArgs: '--user root:root',
        noNode: true,
    ],
    // TODO: add private (that will not go into common config) params section.
    Docman_config: [
        docmanJsonConfigFile: 'config.json',
    ],
    Publish_junit: [
        reportsPath: 'reports/*.xml'
    ],
    JobDslSeed_perform: [
        removedJobAction: 'DELETE',
        removedViewAction: 'DELETE',
        lookupStrategy: 'SEED_JOB',
        additionalClasspath: ['library/src'],
        // TODO: Need another way of providing dsl scripts.
        jobsPattern: ['library/jobdsl/job_dsl_docman.groovy'],
    ],
    Nexus: [
        nexusReleaseType: 'release',
    ],
    Druflow: [
        druflowDir: 'druflow',
        druflowRepo: 'https://github.com/aroq/druflow.git',
    ],
    Druflow_deployFlow: [
        propertiesFile: 'docroot/master/version.properties',
        executeCommand: 'deployFlow',
    ],
    Druflow_copySite: [
        executeCommand: 'dbCopyAC',
    ],
    Druflow_dbBackupSite: [
        executeCommand: 'dbBackupSite',
    ],
    Ansible: [
        ansible_hostsFile: 'docroot/config/ansible/inventory.ini',
    ],
    Ansible_deployWithGit: [
        // debugEnabled: true,
        ansible_playbook: 'library/ansible/deployWithGit.yml',
    ],
    Ansible_deployWithAnsistrano: [
        // debugEnabled: true,
        ansible_playbook: 'library/ansible/deployWithAnsistrano.yml',
    ],
    Common_confirm: [
        timeToConfirm: 60,
    ],
]