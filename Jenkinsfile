pipeline {
    agent any

    environment {
        DOCKERHUB_USER = 'aayodeji'
        DOCKERHUB_REPO = 'boardgame'
        IMAGE_TAG      = "latest"
        K8S_MANIFEST   = 'deployment-service.yaml'
        SCANNER_HOME   = tool 'sonar-scanner'
    }

    tools {
        jdk    'jdk25'
        maven  'maven3'
        nodejs 'nodejs'
    }

    stages {

        stage('Checkout') {
            steps {
                cleanWs()
                checkout scm
            }
        }

        stage('Compile') {
            steps {
                sh 'mvn clean compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }

        stage('Generate Coverage Report') {
            steps {
                sh 'mvn jacoco:report'
                sh 'ls -la target/site/jacoco/jacoco.xml'
            }
        }

        stage('File System Scan') {
            steps {
                sh '''
                    mkdir -p reports
                    trivy fs --format table -o reports/trivy-fs-report.html .
                '''
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh '''
                        $SCANNER_HOME/bin/sonar-scanner \
                            -Dsonar.projectName=BoardGame \
                            -Dsonar.projectKey=BoardGame \
                            -Dsonar.sources=src/main/java,src/main/resources \
                            -Dsonar.tests=src/test/java \
                            -Dsonar.java.binaries=target/classes \
                            -Dsonar.java.libraries=$HOME/.m2/repository/**/*.jar \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                            -Dsonar.coverage.exclusions=src/main/resources/**
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: false
                }
            }
        }

        stage('Publish Artefact to Nexus') {
            steps {
                withMaven(globalMavenSettingsConfig: 'global-settings', jdk: 'jdk25', maven: 'maven3', traceability: true) {
                    sh 'mvn deploy'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'docker-cred',
                    usernameVariable: 'DOCKER_USERNAME',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {
                    sh '''
                        echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
                        docker build -t ${DOCKERHUB_USER}/${DOCKERHUB_REPO}:${IMAGE_TAG} .
                    '''
                }
            }
        }

        stage('Scan Docker Image') {
            steps {
                sh 'trivy image --format table -o reports/trivy-image-report.html ${DOCKERHUB_USER}/${DOCKERHUB_REPO}:${IMAGE_TAG}'
            }
        }

        stage('Push Docker Image') {
            steps {
                sh '''
                    docker push ${DOCKERHUB_USER}/${DOCKERHUB_REPO}:${IMAGE_TAG}
                '''
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withKubeConfig(
                    caCertificate: '',
                    clusterName: 'kubernetes',
                    contextName: '',
                    credentialsId: 'k8-cred',
                    namespace: 'boardgame',
                    restrictKubeConfigAccess: false,
                    serverUrl: 'https://10.0.1.2:6443'
                ) {
                    sh '''
                        sed -i "s|IMAGE_TAG|${IMAGE_TAG}|g" ${K8S_MANIFEST}
                        kubectl apply -f ${K8S_MANIFEST}
                        kubectl get pods -n boardgame
                        kubectl get svc -n boardgame
                    '''
                }
            }
        }

    }
    post {
    always {
        archiveArtifacts artifacts: 'reports/trivy-fs-report.html, reports/trivy-image-report.html',
                         fingerprint: true

        publishHTML(target: [
            allowMissing: false,
            alwaysLinkToLastBuild: true,
            keepAll: true,
            reportDir: 'target/site/jacoco',
            reportFiles: 'index.html',
            reportName: 'JaCoCo Coverage Report'
        ])

        script {
            // Extract coverage percentage from jacoco.xml
            // reads the LINE counter from the XML and calculates percentage
            def coveragePct = 'N/A'
            try {
                def covered = sh(
                    script: '''
                        grep -o 'type="LINE" missed="[0-9]*" covered="[0-9]*"' \
                            target/site/jacoco/jacoco.xml | \
                        awk -F'"' 'BEGIN{m=0;c=0} {m+=$4; c+=$6} \
                            END{if(m+c>0) printf "%.1f", c/(m+c)*100; else print "0"}'
                    ''',
                    returnStdout: true
                ).trim()
                coveragePct = covered + '%'
            } catch (Exception e) {
                coveragePct = 'N/A — report not generated'
            }

            emailext(
                from: 'ayodeji.adeboyejo@gmail.com',
                to: 'ae.adeboyejo@gmail.com',
                subject: "Jenkins Pipeline — ${currentBuild.fullDisplayName} — ${currentBuild.currentResult}",
                body: """
                    <html>
                    <body style="font-family: Arial, sans-serif; font-size: 14px;">

                        <h2 style="color: ${currentBuild.currentResult == 'SUCCESS' ? '#2e7d32' : '#c62828'};">
                            Pipeline ${currentBuild.currentResult} — ${currentBuild.fullDisplayName}
                        </h2>

                        <!-- Build Summary -->
                        <h3>Build Summary</h3>
                        <table style="border-collapse: collapse; width: 70%;">
                            <tr style="background-color: #f5f5f5;">
                                <td style="padding: 8px; border: 1px solid #ddd; width: 35%;"><b>Project</b></td>
                                <td style="padding: 8px; border: 1px solid #ddd;">${env.JOB_NAME}</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px; border: 1px solid #ddd;"><b>Build Number</b></td>
                                <td style="padding: 8px; border: 1px solid #ddd;">#${env.BUILD_NUMBER}</td>
                            </tr>
                            <tr style="background-color: #f5f5f5;">
                                <td style="padding: 8px; border: 1px solid #ddd;"><b>Status</b></td>
                                <td style="padding: 8px; border: 1px solid #ddd;
                                    color: ${currentBuild.currentResult == 'SUCCESS' ? '#2e7d32' : '#c62828'};">
                                    <b>${currentBuild.currentResult}</b>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding: 8px; border: 1px solid #ddd;"><b>Duration</b></td>
                                <td style="padding: 8px; border: 1px solid #ddd;">${currentBuild.durationString}</td>
                            </tr>
                            <tr style="background-color: #f5f5f5;">
                                <td style="padding: 8px; border: 1px solid #ddd;"><b>Docker Image</b></td>
                                <td style="padding: 8px; border: 1px solid #ddd;">
                                    ${env.DOCKERHUB_USER}/${env.DOCKERHUB_REPO}:${env.IMAGE_TAG}
                                </td>
                            </tr>
                            <tr>
                                <td style="padding: 8px; border: 1px solid #ddd;"><b>Branch</b></td>
                                <td style="padding: 8px; border: 1px solid #ddd;">${env.GIT_BRANCH ?: 'main'}</td>
                            </tr>
                        </table>

                        <br>

                        <!-- Code Coverage -->
                        <h3>Code Coverage — JaCoCo</h3>
                        <table style="border-collapse: collapse; width: 70%;">
                            <tr style="background-color: #f5f5f5;">
                                <td style="padding: 8px; border: 1px solid #ddd; width: 35%;"><b>Line Coverage</b></td>
                                <td style="padding: 8px; border: 1px solid #ddd;
                                    color: ${coveragePct != 'N/A' && coveragePct.replace('%','').toFloat() >= 80 ? '#2e7d32' : '#c62828'};">
                                    <b>${coveragePct}</b>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding: 8px; border: 1px solid #ddd;"><b>Full Coverage Report</b></td>
                                <td style="padding: 8px; border: 1px solid #ddd;">
                                    <a href="${env.BUILD_URL}JaCoCo_20Coverage_20Report">
                                        View JaCoCo Report in Jenkins
                                    </a>
                                </td>
                            </tr>
                        </table>

                        <br>

                        <!-- Code Quality -->
                        <h3>Code Quality — SonarQube</h3>
                        <table style="border-collapse: collapse; width: 70%;">
                            <tr style="background-color: #f5f5f5;">
                                <td style="padding: 8px; border: 1px solid #ddd; width: 35%;"><b>SonarQube Dashboard</b></td>
                                <td style="padding: 8px; border: 1px solid #ddd;">
                                    <a href="http://34.74.204.123:9000/dashboard?id=BoardGame">
                                        View Full Analysis on SonarQube
                                    </a>
                                </td>
                            </tr>
                        </table>

                        <br>

                        <!-- Security Scan Summary -->
                        <h3>Security Scans — Trivy</h3>
                        <table style="border-collapse: collapse; width: 70%;">
                            <tr style="background-color: #f5f5f5;">
                                <td style="padding: 8px; border: 1px solid #ddd; width: 35%;"><b>Filesystem Scan</b></td>
                                <td style="padding: 8px; border: 1px solid #ddd;">
                                    Report attached — <b>trivy-fs-report.html</b>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding: 8px; border: 1px solid #ddd;"><b>Docker Image Scan</b></td>
                                <td style="padding: 8px; border: 1px solid #ddd;">
                                    Report attached — <b>trivy-image-report.html</b>
                                </td>
                            </tr>
                        </table>

                        <br>

                        <!-- Links -->
                        <h3>Quick Links</h3>
                        <ul>
                            <li><a href="${env.BUILD_URL}">Jenkins Build Page</a></li>
                            <li><a href="${env.BUILD_URL}console">Console Output</a></li>
                            <li><a href="${env.BUILD_URL}JaCoCo_20Coverage_20Report">JaCoCo Coverage Report</a></li>
                            <li><a href="http://34.74.204.123:9000/dashboard?id=BoardGame">SonarQube Dashboard</a></li>
                        </ul>

                        <br>
                        <p style="color: #757575; font-size: 12px;">
                            This is an automated notification from Jenkins.
                            Build triggered at ${new Date().format('dd MMM yyyy HH:mm:ss z')}.
                        </p>

                    </body>
                    </html>
                """,
                mimeType: 'text/html',
                attachLog: true,
                attachmentsPattern: 'reports/trivy-fs-report.html, reports/trivy-image-report.html'
            )
        }
    }

    success {
        echo "Pipeline completed successfully — image: ${DOCKERHUB_USER}/${DOCKERHUB_REPO}:${IMAGE_TAG}"
    }

    failure {
        echo "Pipeline failed at build ${BUILD_NUMBER}"
    }
}
 
}