FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY Warehouse-Distributed-System/.mvn/ .mvn
COPY Warehouse-Distributed-System/mvnw Warehouse-Distributed-System/pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY Distributed-System-Contracts /contracts
RUN ./mvnw -f /contracts/pom.xml install -DskipTests
COPY Warehouse-Distributed-System/src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]