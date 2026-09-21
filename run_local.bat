@echo off
REM =====================================================
REM  LEALTIX BACKEND - Arranque local (desarrollo)
REM  Setea variables de entorno y lanza Spring Boot
REM =====================================================
setlocal
cd /d "%~dp0"

REM --- Java / Maven ---
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "MAVEN_HOME=C:\Users\lenovo\AppData\Local\Programs\Maven\apache-maven-3.9.9"
set "PATH=%MAVEN_HOME%\bin;%JAVA_HOME%\bin;%PATH%"

REM --- Base de datos local ---
set "SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/lealtix_db"
set "SPRING_DATASOURCE_USERNAME=postgres"
set "SPRING_DATASOURCE_PASSWORD=admin"

REM --- Desarrollo local: Flyway off, Hibernate crea las tablas ---
set "SPRING_FLYWAY_ENABLED=false"
set "SPRING_JPA_HIBERNATE_DDL_AUTO=update"

REM --- Secretos (desarrollo). Reemplazar por valores reales ---
set "JWT_SECRET=cambia-esta-clave-secreta-jwt-muy-larga-de-al-menos-256-bits-para-produccion-1234567890abc"
set "STRIPE_API_KEY=sk_test_TODO_REEMPLAZAR"
set "STRIPE_WEBHOOK_SECRET=whsec_TODO_REEMPLAZAR"
set "SENDGRID_API_KEY=SG.TODO_REEMPLAZAR"
set "CLOUDINARY_CLOUD_NAME=TODO_REEMPLAZAR"
set "CLOUDINARY_API_KEY=TODO_REEMPLAZAR"
set "CLOUDINARY_SECRET=TODO_REEMPLAZAR"
set "CLOUDINARY_URL=cloudinary://TODO:TODO@TODO_REEMPLAZAR"

echo Arrancando backend Lealtix en http://localhost:8080 ...
call mvn spring-boot:run > backend-run.log 2>&1

endlocal
