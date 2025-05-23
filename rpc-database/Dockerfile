# Erste Stage: Build
FROM maven:3.9.9-eclipse-temurin-23 AS build

# Setze Arbeitsverzeichnis
WORKDIR /app

# Kopiere nur pom.xml zuerst für besseres Layer-Caching
COPY pom.xml .

# Lade Dependencies herunter
RUN mvn dependency:go-offline

# Kopiere Quellcode
COPY src ./src

# Baue die Anwendung
RUN mvn clean package -DskipTests

# Zweite Stage: Runtime
FROM eclipse-temurin:23-jre-jammy

# Setze Arbeitsverzeichnis
WORKDIR /app

# Erstelle nicht-root Benutzer
RUN groupadd -r grpcuser && useradd -r -g grpcuser grpcuser

# Kopiere JAR aus Build-Stage
COPY --from=build /app/target/*.jar app.jar

# Setze Besitzer für mehr Sicherheit
RUN chown -R grpcuser:grpcuser /app

# Wechsle zu nicht-root Benutzer
USER grpcuser

# Exponiere gRPC-Port
EXPOSE 50051

# Health-Check (prüft ob Java-Prozess läuft)
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD pgrep -f "java.*app.jar" > /dev/null || exit 1

# Setze JVM-Optionen für Container-Umgebung
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Dfile.encoding=UTF-8"

# Starte Anwendung
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Metadaten
LABEL maintainer="Team A" \
      description="gRPC Database Service" \
      version="1.0"