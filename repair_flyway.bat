@echo off
echo ========================================
echo Flyway Repair Script
echo ========================================
echo.
echo This script will repair the Flyway schema history table
echo to fix checksum mismatches for migrations.
echo.
echo Make sure you have:
echo - PostgreSQL running on localhost:5432
echo - Database: lealtix_db
echo - User: postgres / Password: admin
echo.
pause

REM Load environment variables from environment.env file
for /f "tokens=1,2 delims==" %%a in (environment.env) do (
    if "%%a"=="DB_USER" set DB_USER=%%b
    if "%%a"=="DB_PASSWORD" set DB_PASSWORD=%%b
    if "%%a"=="DB_HOST" set DB_HOST=%%b
    if "%%a"=="DB_NAME" set DB_NAME=%%b
)

REM Default values if not found in environment.env
if not defined DB_USER set DB_USER=postgres
if not defined DB_PASSWORD set DB_PASSWORD=admin
if not defined DB_HOST set DB_HOST=localhost
if not defined DB_NAME set DB_NAME=lealtix_db

echo.
echo Connecting to database: postgresql://%DB_HOST%:5432/%DB_NAME%
echo User: %DB_USER%
echo.

REM Execute Flyway repair using Maven
echo Running: mvn flyway:repair
echo.
call mvn flyway:repair -Dflyway.url=jdbc:postgresql://%DB_HOST%:5432/%DB_NAME% -Dflyway.user=%DB_USER% -Dflyway.password=%DB_PASSWORD% -Dflyway.locations=classpath:db/migration

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Flyway repair completed successfully!
    echo ========================================
    echo.
    echo The schema history table has been repaired.
    echo You can now run your application.
    echo.
) else (
    echo.
    echo ========================================
    echo Flyway repair failed!
    echo ========================================
    echo.
    echo Please check the error messages above.
    echo Error code: %ERRORLEVEL%
    echo.
)

echo.
pause
