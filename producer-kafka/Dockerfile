FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src ./src

RUN if [ -f mvnw ]; then chmod +x mvnw; fi

RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre AS runtime

ENV APP_HOME=/app
WORKDIR $APP_HOME

ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENV KAFKA_BOOTSTRAP_SERVERS=broker1:9092,broker2:9092,broker3:9092
ENV TOPIC_NAME=my-topic

COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
