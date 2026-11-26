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

echo El navegador se abrira cuando TimeForge este listo.
echo (Para detener la aplicacion, simplemente cierra esta ventana o pulsa Ctrl + C)
echo.

"%JAVA_CMD%" -jar target\timeforge.jar --timeforge.abrir-navegador=true
if errorlevel 1 (
    echo.
    echo No se pudo iniciar TimeForge. Revisa el error anterior.
    echo Si H2 indica que la base de datos esta en uso, pulsa Disconnect en la consola H2.
)

pause
