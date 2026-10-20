@echo off
chcp 65001 >nul
title Invio Assistente_Meditech_Mobile.apk su Telegram
echo ====================================================================
echo      INVIO DIRETTO DELL'APP MEDITECH (.APK) SUL TUO TELEGRAM
echo ====================================================================
echo.

set "APK_FILE=%~dp0Assistente_Meditech_Mobile.apk"
if not exist "%APK_FILE%" (
    set "APK_FILE=%~dp0app\build\outputs\apk\debug\app-debug.apk"
)

if not exist "%APK_FILE%" (
    echo [ERRORE] File Assistente_Meditech_Mobile.apk non trovato! Esegui prima COMPILA_APK.bat.
    pause
    exit /b 1
)

echo Invio di Assistente_Meditech_Mobile.apk alla tua chat Telegram in corso...
echo Attendi qualche secondo...
curl.exe -s -F "chat_id=503515287" -F "document=@%APK_FILE%" -F "caption=🏥 Ecco l'app nativa Assistente MeDiTech Mobile (SUPSI MeDiTech) .APK! Tocca il file dal telefono per installarla." "https://api.telegram.org/bot8981584935:AAHETlQ89f0CQ11q6qQFzCEBf12-TXWJjaY/sendDocument" > "%TEMP%\tg_meditech_res.json"

findstr /C:"\"ok\":true" "%TEMP%\tg_meditech_res.json" >nul
if %ERRORLEVEL% EQU 0 (
    echo.
    echo [OK] APK inviato con successo sul tuo Telegram!
    echo Apri Telegram sullo smartphone e tocca il file per installare l'app.
) else (
    echo.
    echo [AVVISO] Invio completato o risposta API:
    type "%TEMP%\tg_meditech_res.json"
)
echo.
pause
