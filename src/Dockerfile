FROM gradle:jdk17 as builder

WORKDIR /home/gradle/src
RUN curl -OL https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.4.3/applicationinsights-agent-3.4.3.jar
COPY --chown=gradle:gradle . /home/gradle/src/
RUN gradle build

FROM mcr.microsoft.com/openjdk/jdk:17-ubuntu

EXPOSE 8080
WORKDIR /app
COPY --from=builder /home/gradle/src/applicationinsights-agent-3.4.3.jar ./applicationinsights-agent-3.4.3.jar
COPY --from=builder /home/gradle/src/build/libs/*.jar ./app.jar
ENTRYPOINT ["java", "-Xmx128M", "-XX:+FlightRecorder", "-XX:+PrintFlagsFinal", "-javaagent:/app/applicationinsights-agent-3.4.3.jar", "-jar", "app.jar"]