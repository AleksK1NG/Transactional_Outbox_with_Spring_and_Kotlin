FROM --platform=linux/arm64 azul/zulu-openjdk-alpine:17 as builder
ARG JAR_FILE=build/libs/orders-microservice-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM azul/zulu-openjdk-alpine:17
COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder spring-boot-loader/ ./
COPY --from=builder application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher", "-XX:MaxRAMPercentage=75", "-XX:+UseG1GC"]