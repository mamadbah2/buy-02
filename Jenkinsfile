// filepath: /home/mamadbah/Java/mr-jenk/Jenkinsfile.optimized
pipeline {
    agent any

    // NOTE: Tous les stages s'exécutent sur le même agent (Jenkins choisit un agent disponible)

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 60, unit: 'MINUTES')
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        DOCKER_HUB_USER = 'mamadbah2'
        IMAGE_VERSION = "${env.BUILD_NUMBER}"
        GITHUB_TOKEN = credentials('GITHUB_TOKEN')

        // Préfixe pour éviter les conflits entre projets
        PROJECT_NAME = 'buy-02'

        // Media Service credentials
        MONGODB_URI = credentials('MONGODB_URI_BOBO')
        MONGODB_DATABASE = credentials('MONGODB_DATABASE')
        SUPABASE_PROJECT_URL = credentials('SUPABASE_PROJECT_URL')
        SUPABASE_API_KEY = credentials('SUPABASE_API_KEY')
        SUPABASE_BUCKET_NAME = credentials('SUPABASE_BUCKET_NAME')

        // Cache directories - améliore la performance
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository -Dmaven.artifact.threads=10'
        NPM_CONFIG_CACHE = '.npm-cache'
    }

    stages {

        stage('Clean Workspace') {
            steps {
                echo '🧹 Nettoyage de l\'espace de travail...'
                sh '''
                    # Nettoyer seulement les conteneurs et images de l'ancien build
                    docker-compose down -v --remove-orphans || true
                    docker images | grep "my_buy01_pipeline2" | grep -v "${IMAGE_VERSION}" | awk '{print $3}' | xargs -r docker rmi -f || true
                '''
            }
        }

        stage('Build & Unit Tests') {
            parallel {
                stage('Frontend') {
                    steps {
                        echo '🧪 Tests Frontend Angular (Headless)...'
                        dir('buy-01-frontend') {
                            sh '''
                                npm ci --cache ${NPM_CONFIG_CACHE}
                                npm run test:headless
                            '''
                        }
                    }
                }

                stage('Backend Services') {
                    steps {
                        echo '🚀 Build et Tests des Services Backend...'
                        script {
                            def services = ['discovery-service', 'config-service', 'api-gateway', 'product-service', 'user-service', 'media-service']
                            services.each { svc ->
                                echo "🔨 Build et test de ${svc}..."
                                sh """
                                    mvn -f ${svc}/pom.xml clean verify \
                                        --batch-mode \
                                        -Dmaven.test.failure.ignore=false
                                """
                            }
                        }
                    }
                }
            }
            post {
                always {
                    // Les rapports JUnit et les artefacts proviennent des agents; nous archiveons depuis le contrôleur
                    // pour éviter les problèmes d'accès, on copie d'abord les artefacts si nécessaire.
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
                }
            }
        }

       // groovy
       stage('Code Quality (Sonar + Gate)') {
           steps {
               echo '🔍 Analyses Sonar en parallèle + attente des Quality Gates...'
               script {
                   def services = ['discovery-service','config-service','api-gateway','product-service','user-service','media-service']
                   def parallelQuality = [:]

                   // Backend services
                   services.each { svc ->
                       parallelQuality[svc] = {
                           echo "🔎 Sonar pour ${svc}..."
                           dir(svc) {
                               // Move the Sonar wrapper and credentials inside each parallel branch
                               withSonarQubeEnv('safe-zone-mr-jenk') {
                                   withCredentials([string(credentialsId: 'SONAR_USER_TOKEN', variable: 'SONAR_USER_TOKEN')]) {
                                       def jacocoOption = (svc in ['product-service','user-service','media-service']) ?
                                           "-Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml" : ""

                                       sh """
                                           mvn sonar:sonar \
                                               -Dsonar.projectKey=sonar-${svc.replace('-service','')} \
                                               -Dsonar.host.url=$SONAR_HOST_URL \
                                               -Dsonar.token=$SONAR_USER_TOKEN \
                                               -Dsonar.java.binaries=target/classes \
                                               ${jacocoOption}
                                       """
                                   } // withCredentials
                               } // withSonarQubeEnv

                               // waitForQualityGate must run in the same workspace/context
                               timeout(time: 10, unit: 'MINUTES') {
                                   waitForQualityGate abortPipeline: true
                               }
                           } // dir
                       }
                   }

                   // Frontend Angular
                   parallelQuality['frontend'] = {
                       echo "🔎 Sonar pour Frontend Angular..."
                       dir('buy-01-frontend') {
                           withSonarQubeEnv('safe-zone-mr-jenk') {
                               withCredentials([string(credentialsId: 'SONAR_USER_TOKEN', variable: 'SONAR_USER_TOKEN')]) {
                                   sh """
                                       npm install --save-dev sonarqube-scanner --cache ${NPM_CONFIG_CACHE}

                                       npx sonar-scanner \
                                           -Dsonar.projectKey=sonar-frontend \
                                           -Dsonar.sources=src \
                                           -Dsonar.tests=src \
                                           -Dsonar.test.inclusions=**/*.spec.ts \
                                           -Dsonar.exclusions=**/node_modules/**,**/coverage/**,**/dist/**,**/*.spec.ts \
                                           -Dsonar.typescript.lcov.reportPaths=coverage/buy-01-frontend/lcov.info \
                                           -Dsonar.host.url=$SONAR_HOST_URL \
                                           -Dsonar.token=$SONAR_USER_TOKEN
                                    """
                                }
                            }
                            // waitForQualityGate must run in the same workspace/context
                            timeout(time: 10, unit: 'MINUTES') {
                                waitForQualityGate abortPipeline: true
                            }
                        }
                    }

                   parallel parallelQuality
               }
           }
       }



        stage('Build Docker Images') {
            steps {
                echo '🐳 Construction des images Docker en parallèle...'
                script {
                    def services = ['eureka-server', 'config-service', 'api-gateway', 'product-service', 'user-service', 'media-service', 'frontend']
                    def parallelBuilds = [:]

                    services.each { service ->
                        parallelBuilds[service] = {
                            def serviceDir = service == 'frontend' ? 'buy-01-frontend' : service.replace('eureka-server', 'discovery-service')
                            echo "🔨 Construction de ${service}..."
                            sh """
                                docker build -t my_buy01_pipeline2-${service}:latest \
                                    --build-arg MAVEN_OPTS="-Dmaven.test.skip=true" \
                                    --cache-from ${DOCKER_HUB_USER}/${PROJECT_NAME}-${service}:latest \
                                    -f ${serviceDir}/Dockerfile \
                                    ${serviceDir}
                            """
                        }
                    }

                    parallel parallelBuilds
                }
            }
        }

        stage('Integration Tests') {
            steps {
                echo '🧪 Tests d\'intégration...'
                script {
                    timeout(time: 10, unit: 'MINUTES') {
                        try {
                            withEnv([
                                "IMAGE_VERSION=${env.BUILD_NUMBER}",
                                "PROJECT_NAME=${PROJECT_NAME}",
                                "GITHUB_TOKEN=${env.GITHUB_TOKEN}",
                                "SUPABASE_PROJECT_URL=${env.SUPABASE_PROJECT_URL}",
                                "SUPABASE_API_KEY=${env.SUPABASE_API_KEY}",
                                "SUPABASE_BUCKET_NAME=${env.SUPABASE_BUCKET_NAME}",
                                "MONGODB_URI=${env.MONGODB_URI}",
                                "MONGODB_DATABASE=${env.MONGODB_DATABASE}"
                            ]) {
                                sh '''
                                    docker-compose up -d

                                    # Attendre que les services soient prêts avec health checks
                                    echo "⏳ Attente du démarrage des services..."
                                    for i in {1..40}; do
                                        if docker-compose ps | grep -E "(healthy|running)" | wc -l | grep -q 7; then
                                            echo "✅ Tous les services sont démarrés"
                                            break
                                        fi
                                        echo "Tentative $i/40..."
                                        sleep 5
                                    done

                                    # Vérifier que les services sont en bonne santé
                                    docker-compose ps
                                '''
                            }
                        } finally {
                            sh 'docker-compose logs --tail=50'
                            sh 'docker-compose down -v --remove-orphans'
                        }
                    }
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                script {
                    echo '📤 Push des images vers Docker Hub...'

                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credential',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
                    }

                    def services = ['frontend', 'product-service', 'user-service', 'media-service', 'api-gateway', 'config-service', 'eureka-server']
                    def parallelPushes = [:]

                    services.each { service ->
                        parallelPushes[service] = {
                            def localImageName = "my_buy01_pipeline2-${service}"
                            // Préfixer avec PROJECT_NAME pour éviter les conflits entre applications
                            def taggedImageName = "${DOCKER_HUB_USER}/${PROJECT_NAME}-${service}:${env.BUILD_NUMBER}"
                            def latestImageName = "${DOCKER_HUB_USER}/${PROJECT_NAME}-${service}:latest"

                            echo "📦 Push de ${service}..."
                            sh """
                                docker tag ${localImageName}:latest ${taggedImageName}
                                docker tag ${localImageName}:latest ${latestImageName}
                                docker push ${taggedImageName}
                                docker push ${latestImageName}
                            """
                        }
                    }

                    parallel parallelPushes

                    sh 'docker logout'
                }
            }
        }

        stage('Deploy Locally') {
            steps {
                script {
                    echo "🚀 Déploiement local, version ${env.BUILD_NUMBER}..."

                    timeout(time: 10, unit: 'MINUTES') {
                        withEnv([
                            "IMAGE_VERSION=${env.BUILD_NUMBER}",
                            "PROJECT_NAME=${PROJECT_NAME}",
                            "GITHUB_TOKEN=${env.GITHUB_TOKEN}",
                            "SUPABASE_PROJECT_URL=${env.SUPABASE_PROJECT_URL}",
                            "SUPABASE_API_KEY=${env.SUPABASE_API_KEY}",
                            "SUPABASE_BUCKET_NAME=${env.SUPABASE_BUCKET_NAME}",
                            "MONGODB_URI=${env.MONGODB_URI}",
                            "MONGODB_DATABASE=${env.MONGODB_DATABASE}"
                        ]) {
                            sh '''
                                docker-compose -f docker-compose-deploy.yml down
                                docker-compose -f docker-compose-deploy.yml pull
                                docker-compose -f docker-compose-deploy.yml up -d

                                # Vérifier l'état des conteneurs
                                echo "⏳ Attente du démarrage..."
                                for i in {1..20}; do
                                    if docker-compose -f docker-compose-deploy.yml ps | grep -q "Up"; then
                                        echo "✅ Services démarrés"
                                        break
                                    fi
                                    echo "Tentative $i/20..."
                                    sleep 3
                                done
                                docker-compose -f docker-compose-deploy.yml ps
                            '''
                        }
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    echo '🏥 Vérification de la santé des services...'
                    timeout(time: 5, unit: 'MINUTES') {
                        sh '''
                            # Attendre que tous les services soient en bonne santé
                            for i in {1..30}; do
                                if docker-compose -f docker-compose-deploy.yml ps | grep -q "unhealthy"; then
                                    echo "⏳ Attente de la santé des services... ($i/30)"
                                    sleep 10
                                else
                                    echo "✅ Tous les services sont en bonne santé"
                                    exit 0
                                fi
                            done

                            echo "❌ Timeout: certains services ne sont pas en bonne santé"
                            docker-compose -f docker-compose-deploy.yml ps
                            exit 1
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                // s'assurer que les commandes Docker du post s'exécutent sur le noeud contrôleur
                node {
                    // Nettoyer les anciennes images
                    sh '''
                        docker images | grep "${DOCKER_HUB_USER}" | grep -v "${IMAGE_VERSION}" | grep -v "latest" | awk '{print $3}' | xargs -r docker rmi -f || true
                    '''

                    mail(
                        to: 'bahmamadoubobosewa@gmail.com',
                        subject: "SUCCESS: Pipeline ${env.JOB_NAME} [${env.BUILD_NUMBER}]",
                        body: "Le pipeline a réussi.\nJob: ${env.JOB_NAME}\nBuild: ${env.BUILD_NUMBER}\nVoir les détails: ${env.BUILD_URL}"
                    )
                }
            }
        }

        failure {
            script {
                echo "⚠️ Pipeline échouée, rollback en cours..."

                def lastSuccessfulBuild = currentBuild.previousSuccessfulBuild

                node {
                    if (lastSuccessfulBuild && lastSuccessfulBuild.number != env.BUILD_NUMBER) {
                        echo "🔄 Rollback vers la version ${lastSuccessfulBuild.number}..."
                        try {
                            withEnv(["IMAGE_VERSION=${lastSuccessfulBuild.number}"]) {
                                sh '''
                                    docker-compose -f docker-compose-deploy.yml down
                                    docker-compose -f docker-compose-deploy.yml pull
                                    docker-compose -f docker-compose-deploy.yml up -d
                                '''
                            }
                            echo "✅ Rollback réussi vers la version ${lastSuccessfulBuild.number}"
                        } catch (Exception e) {
                            echo "❌ Échec du rollback: ${e.message}"
                        }
                    } else {
                        echo "⚠️ Aucune version précédente disponible pour rollback."
                    }

                    mail(
                        to: 'bahmamadoubosewa@gmail.com',
                        subject: "FAILURE: Pipeline ${env.JOB_NAME} [${env.BUILD_NUMBER}]",
                        body: "Le pipeline a echoue.\nJob: ${env.JOB_NAME}\nBuild: ${env.BUILD_NUMBER}\nVoir les détails: ${env.BUILD_URL}"
                    )
                }
            }
        }

        always {
            script {
                node {
                    echo '🧹 Nettoyage final...'
                    sh '''
                        # Nettoyer les conteneurs arrêtés et les images dangereuses
                        docker container prune -f || true
                        docker image prune -f || true
                    '''
                }
            }
        }
    }
}
