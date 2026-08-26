@echo off
cd /d "%~dp0"
echo Running Maven Tests...
call mvn clean test -DskipITs
echo.
echo Test run completed.
pause
