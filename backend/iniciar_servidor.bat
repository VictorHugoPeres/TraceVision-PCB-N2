@echo off
chcp 65001 > nul
title TraceVision - Servidor Backend REST
echo ====================================================================
echo             TRACEVISION - SERVIDOR BACKEND REST (PORTA 8080)
echo ====================================================================
echo.

REM Verifica e configura o ADB se o celular estiver conectado no USB
set ADB_EXE=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
if exist "%ADB_EXE%" (
    echo [ADB] Configurando redirecionamento via cabo USB...
    "%ADB_EXE%" reverse tcp:8080 tcp:8080 >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo [ADB] CELULAR RECONHECIDO NO CABO USB!
        echo       Voce pode usar no celular: http://localhost:8080/
    )
)

echo.
echo [IP WI-FI] No celular via Wi-Fi, use: http://10.10.10.113:8080/
echo [CABO USB] No celular via USB, use:   http://localhost:8080/
echo [EMULADOR] No emulador Android, use: http://10.0.2.2:8080/
echo.
echo ====================================================================
echo   INICIANDO SERVIDOR PYTHON COM BANCO DE DADOS SQLITE...
echo   (NÃO FECHE ESTA JANELA ENQUANTO TESTAR O APLICATIVO)
echo ====================================================================
echo.

python "%~dp0server.py"
pause
