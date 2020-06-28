node {

    stage ('SCM Checkout'){
        
        git 'https://github.com/SalmonRanjay/spring-mvc-toolkit.git'
    }

    stage ('Compile-Package'){
        // returns maven home directory
        def mvnHome = tool name: 'maven-3', type: 'maven'

        sh "${mvnHome}/bin/mvn clean package"
    }
}