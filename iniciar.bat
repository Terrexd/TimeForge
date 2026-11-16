@echo off
title TimeForge - Servidor
cd /d "%~dp0"

echo ==============================================
echo       Iniciando TimeForge Application...
echo ==============================================
echo.

set "JAVA_CMD=java"
if exist "C:\Program Files\Java\jdk-27\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Java\jdk-27\bin\java.exe"
)

echo Abriendo navegador en http://localhost:8080 ...
timeout /t 2 /nobreak >nul
start http://localhost:8080

echo.
echo Iniciando servidor en el puerto 8080...
echo (Para detener la aplicacion, simplemente cierra esta ventana o pulsa Ctrl + C)
echo.

"%JAVA_CMD%" -jar target\timeforge-1.0.0.jar

pause
