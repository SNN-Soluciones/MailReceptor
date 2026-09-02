# Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Los tests corren en el CI (ci.yml); acá solo se empaqueta.
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.war app.war
# Sin privilegios: el worker solo necesita leer su propio WAR y salir a IMAP/HTTPS.
# Correr como root en el contenedor amplía el daño de cualquier RCE en un parser.
RUN groupadd --system mailreceptor && useradd --system --gid mailreceptor --no-create-home mailreceptor \
    && chown -R mailreceptor:mailreceptor /app
USER mailreceptor
ENV JAVA_OPTS="-Xmx256m -Xms128m"
EXPOSE 5002
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.war"]
