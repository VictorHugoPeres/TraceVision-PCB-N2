@echo off
chcp 65001 > nul
title TraceVision - Subir para o GitHub
echo ====================================================================
echo             TRACEVISION - UPLOAD PARA O GITHUB
echo ====================================================================
echo.
echo 1. Crie um repositório NOVO e VAZIO na sua conta do GitHub:
echo    https://github.com/new
echo    (Deixe sem README, sem .gitignore e sem licença)
echo.
echo 2. Copie o link HTTPS do seu repositório (exemplo:
echo    https://github.com/seu-usuario/TrabalhodaN2.git)
echo.
set /p REPO_URL="Cole a URL do seu repositório aqui e aperte ENTER: "

if "%REPO_URL%"=="" (
    echo.
    echo [ERRO] Nenhuma URL informada. Abortando.
    pause
    exit /b
)

echo.
echo [1/4] Configurando branch principal como main...
git branch -M main

echo [2/4] Configurando repositório remoto...
git remote remove origin 2>nul
git remote add origin %REPO_URL%

echo [3/4] Adicionando arquivos e preparando commit...
git add .
git commit -m "feat: Implementação completa TraceVision N2 (Android Jetpack Compose + YOLO ONNX + Backend REST)" 2>nul

echo [4/4] Enviando arquivos para o GitHub...
git push -u origin main

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ====================================================================
    echo [SUCESSO] Seu projeto foi enviado para o GitHub com sucesso!
    echo ====================================================================
) else (
    echo.
    echo [ATENÇÃO] Ocorreu um problema ao enviar. Verifique se você está
    echo conectado na sua conta do GitHub no navegador/Git Credential Manager.
)

echo.
pause
