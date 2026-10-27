# Build stage: JDK 25 + Maven 3.9.9
FROM eclipse-temurin:25 AS build

WORKDIR /workspace

# Install Maven manually
RUN apt-get update && \
    apt-get install -y wget tar && \
    wget https://downloads.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz && \
    tar -xvzf apache-maven-3.9.9-bin.tar.gz -C /opt && \
    ln -s /opt/apache-maven-3.9.9 /opt/maven && \
    rm apache-maven-3.9.9-bin.tar.gz && \
    apt-get clean

ENV MAVEN_HOME=/opt/maven
ENV PATH=${MAVEN_HOME}/bin:${PATH}

# Copy only pom.xml first to leverage dependency caching
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -B -DskipTests package

# Runtime stage: slim JRE 25
FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app
RUN apk add --no-cache curl
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build --chown=spring:spring /workspace/target/taskmanager-*.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
