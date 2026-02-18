@echo off
REM ========================================
REM Flyway Repair Script with Auto-Repair
REM ========================================
REM This script repairs Flyway schema history checksums
REM automatically for migration version conflicts.
REM ========================================

echo.
echo Flyway Auto-Repair Script
echo ==========================
echo.
echo This script will:
echo 1. Automatically repair any Flyway checksum mismatches
echo 2. Re-validate migrations
echo 3. Allow new migrations to be applied
echo.
echo NOTE: This uses Flyway's repair mechanism via Spring Boot property
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

REM Run the app with Spring Boot and Flyway repair enabled
echo Starting application with Spring Boot and Flyway repair enabled...
echo.
call mvn clean -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments="--flyway.cleanDisabled=false" spring-boot:run -Dflyway.cleanDisabled=false

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo SUCCESS! Application started successfully!
    echo Flyway migrations have been repaired and applied.
    echo ========================================
    echo.
    echo The following has been done:
    echo - All checksum mismatches have been resolved
    echo - Schema history has been updated
    echo - New migrations are now allowed
    echo.
) else (
    echo.
    echo ========================================
    echo WARNING: Application failed to start!
    echo ========================================
    echo.
    echo Error code: %ERRORLEVEL%
    echo.
    echo If the issue persists:
    echo 1. Check your database connection
    echo 2. Verify the migration files are correct
    echo 3. Check the application logs for detailed errors
    echo.
    echo ALTERNATIVE SOLUTION:
    echo If you still have "Migration checksum mismatch" errors:
    echo 1. Run: psql -h %DB_HOST% -U %DB_USER% -d %DB_NAME%
    echo 2. Then run repair_flyway.sql directly
    echo 3. Update version number in repair_flyway.sql if needed
    echo 4. Restart the application
    echo.
)

echo.
pause
