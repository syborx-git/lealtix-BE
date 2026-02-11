@echo off
REM ========================================
REM Flyway Repair Script (Spring Boot Profile)
REM ========================================
REM This script repairs Flyway schema history checksums
REM by running the application with Flyway repair enabled.
REM ========================================

echo.
echo Flyway Repair via Spring Boot
echo ==============================
echo.
echo This will update the Flyway schema history checksum
echo for migration version 4 to match the local file.
echo.
pause

REM Load environment variables from environment.env if it exists
if exist environment.env (
    echo Loading environment variables from environment.env...
    for /f "tokens=1,2 delims==" %%a in (environment.env) do (
        if "%%a"=="DB_USER" set DB_USER=%%b
        if "%%a"=="DB_PASSWORD" set DB_PASSWORD=%%b
        if "%%a"=="DB_HOST" set DB_HOST=%%b
        if "%%a"=="DB_NAME" set DB_NAME=%%b
    )
)

REM Set defaults if not loaded from environment.env
if not defined DB_USER set DB_USER=postgres
if not defined DB_PASSWORD set DB_PASSWORD=admin
if not defined DB_HOST set DB_HOST=localhost
if not defined DB_NAME set DB_NAME=lealtix_db

echo.
echo Database Configuration:
echo Host: %DB_HOST%
echo Port: 5432
echo Database: %DB_NAME%
echo User: %DB_USER%
echo.
pause

REM Run the app with Spring Boot (Flyway will execute on startup)
echo Starting application with Spring Boot...
echo.
call mvn -Dspring-boot.run.profiles=dev spring-boot:run

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Application started successfully!
    echo Flyway migrations have been applied.
    echo ========================================
    echo.
) else (
    echo.
    echo ========================================
    echo Application failed to start!
    echo ========================================
    echo.
    echo Error code: %ERRORLEVEL%
    echo.
    echo If you see "Migration checksum mismatch":
    echo 1. Connect to your database directly
    echo 2. Run: UPDATE flyway_schema_history SET success=false WHERE version=4;
    echo 3. Then delete the V004 or V4 file that doesn't match
    echo 4. Run this script again
    echo.
)

echo.
pause
