FROM openjdk:8-jre-alpine
#FROM java:8
ADD web-order-service-v6-0.0.6-SNAPSHOT.jar app.jar
ADD dd-java-agent.jar dd-java-agent.jar
#RUN bash -c 'touch /app.jar'
CMD ["/usr/bin/java","-javaagent:dd-java-agent.jar","-Ddd.service.name=Web-order-Service-kubernetes","-Djava.security.egd=file:/dev/./urandom","-Dspring.profiles.active=container","-jar","/app.jar"]
#ENTRYPOINT ["java","-javaagent:dd-java-agent.jar","-Ddd.service.name=Web-order-Service-kubernetes","-Djava.security.egd=file:/dev/./urandom","-Dspring.profiles.active=container","-jar","/app.jar"]
#ENTRYPOINT ["java","-javaagent:/NewRelicFiles/newrelic.jar","-Djava.security.egd=file:/dev/./urandom","-Dspring.profiles.active=container","-jar","/app.jar"]
