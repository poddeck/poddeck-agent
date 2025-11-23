FROM eclipse-temurin:25.0.1_8-jdk

COPY /build/libs/agent-1.0.0-SNAPSHOT.jar agent.jar
COPY config.ini config.ini

USER 1000

ENTRYPOINT ["java", "-jar", "agent.jar"]