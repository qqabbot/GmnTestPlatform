pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: '要构建/部署的 Git 分支')
        booleanParam(name: 'BUILD_NO_CACHE', defaultValue: false, description: '构建时使用 docker compose build --no-cache')
        choice(
            name: 'DOCKER_COMPOSE_CMD',
            choices: ['docker compose', 'sudo docker compose'],
            description: 'Docker Compose 命令（无 sudo / 有 sudo）'
        )
    }

    environment {
        COMPOSE_CMD = "${params.DOCKER_COMPOSE_CMD}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    if (params.BRANCH?.trim()) {
                        sh "git fetch origin && git checkout ${params.BRANCH} && git pull origin ${params.BRANCH}"
                    }
                }
                sh "git rev-parse --short HEAD"
                echo "已检出分支: ${params.BRANCH}"
            }
        }

        stage('Build') {
            steps {
                script {
                    def noCache = params.BUILD_NO_CACHE ? '--no-cache' : ''
                    sh "${env.COMPOSE_CMD} build ${noCache}".trim()
                }
            }
        }

        stage('Deploy') {
            steps {
                sh "${env.COMPOSE_CMD} up -d"
                echo "部署完成: 前端 http://<服务器IP>:5000  后端 http://<服务器IP>:4000/api"
            }
        }
    }

    post {
        success {
            echo "GMN 测试平台部署成功"
        }
        failure {
            echo "部署失败，请查看日志"
        }
        always {
            sh "${env.COMPOSE_CMD} ps" || true
        }
    }
}
