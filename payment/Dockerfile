FROM eclipse-temurin:21

RUN groupadd -g 1000 appgroup && \
    useradd -r -u 1000 -g appgroup appuser

WORKDIR /app

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /app/app.jar

RUN chown appuser:appgroup /app/app.jar

USER appuser

ENTRYPOINT ["java","-jar","/app/app.jar"]
