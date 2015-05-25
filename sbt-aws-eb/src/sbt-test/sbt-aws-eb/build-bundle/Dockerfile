FROM chatwork/ubuntu-java

ADD target/scala-2.10/build-bundle_2.10-0.1-SNAPSHOT.jar .

EXPOSE 8080

CMD ["java", "build-bundle_2.10-0.1-SNAPSHOT.jar", "HelloWorld"]