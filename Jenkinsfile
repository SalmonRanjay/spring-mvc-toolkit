node {

    stage ('SCM Checkout'){
        git 'https://github.com/SalmonRanjay/spring-mvc-toolkit.git'
    }

    stage ('Compile-Package'){
        sh 'mvn clean package'
    }
}