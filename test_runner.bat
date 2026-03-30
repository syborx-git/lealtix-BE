@echo off
REM Ejecutar tests del proyecto Lealtix Service
cd /d "%~dp0"

echo ============================================
echo Ejecutando Tests Maven
echo ============================================
echo.

call mvn clean test -DskipITs

echo.
echo ============================================
echo Tests completados
echo ============================================
pause
