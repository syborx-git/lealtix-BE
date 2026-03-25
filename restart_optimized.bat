@echo off
REM ========================================
REM Script para reiniciar el Backend de Lealtix
REM optimizado para limpiar cache y recompilar
REM ========================================

echo.
echo ========================================
echo   REINICIO OPTIMIZADO - Lealtix Backend
echo ========================================
echo.

echo [1/4] Deteniendo aplicacion...
echo Presiona Ctrl+C en la ventana del servidor si esta corriendo
timeout /t 3 /nobreak >nul

echo.
echo [2/4] Limpiando compilacion anterior...
call mvn clean
if %errorlevel% neq 0 (
    echo ERROR: Fallo la limpieza del proyecto
    pause
    exit /b 1
)

echo.
echo [3/4] Compilando proyecto (sin tests)...
call mvn install -DskipTests
if %errorlevel% neq 0 (
    echo ERROR: Fallo la compilacion del proyecto
    pause
    exit /b 1
)

echo.
echo [4/4] Iniciando servidor optimizado...
echo.
echo ========================================
echo   Servidor iniciando en puerto 8080
echo   Presiona Ctrl+C para detener
echo ========================================
echo.

call mvn spring-boot:run

pause
