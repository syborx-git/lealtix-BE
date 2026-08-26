# ==============================================================================
# LEALTIX BACKEND - DOCKERFILE OPTIMIZADO (SPRING BOOT 3 / JAVA 17)
# ==============================================================================

# Etapa 1: Build con Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copiar pom.xml primero para aprovechar la caché de dependencias
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Imagen de ejecución ultra-liviana (JRE Alpine)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Instalar curl para healthchecks del contenedor
RUN apk add --no-cache curl

# Copiar el jar compilado
COPY --from=build /app/target/*.jar app.jar

# Configuración de memoria JVM ajustada para VPS compartido de 8GB RAM:
# - Heap inicial: 256MB, Heap máximo: 512MB
# - Metaspace acotado y Garbage Collector G1GC
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:MetaspaceSize=96m -XX:MaxMetaspaceSize=192m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=dev
ENV SERVER_PORT=8080

EXPOSE 8080

# Usar exec para que las señales de parada (SIGTERM) se envíen directamente a Java
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -Dserver.port=${SERVER_PORT} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -jar app.jar"]

