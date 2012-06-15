#Build
    mvn clean package -DskipTests

#Execute
    java -Dsfdc.endpoint=https://login.salesforce.com -Dsfdc.user=user@salesforce -Dsfdc.pass=password -Dbackup.dir=/var/lib/jenkins/backup -Dbackup.desc="api-jenkins config backup" -jar target/chatterbackup.jar