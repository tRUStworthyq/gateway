FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY ../rootCA.pem /tmp/rootCA.pem
RUN keytool -importcert -noprompt -trustcacerts \
    -alias mkcert-root \
    -file /tmp/rootCA.pem \
    -keystore "$JAVA_HOME/lib/security/cacerts" \
    -storepass changeit \
    && rm /tmp/rootCA.pem
ENTRYPOINT ["java","-jar","app.jar"]