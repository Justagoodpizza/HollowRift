@echo off
setlocal

set PROJECT_DIR=%~dp0
set WRAPPER_JAR=%USERPROFILE%\.m2\wrapper\maven-wrapper.jar
set WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar

if not exist "%WRAPPER_JAR%" (
    echo Downloading Maven Wrapper...
    powershell -NoProfile -Command "& { $url='%WRAPPER_URL%'; $out='%WRAPPER_JAR%'; $dir=Split-Path $out; if(-not(Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }; try { Invoke-WebRequest -Uri $url -OutFile $out -UseBasicParsing } catch { Write-Error ('Failed: ' + $_.Exception.Message); exit 1 } }"
    if errorlevel 1 (
        echo Error: Failed to download
        exit /b 1
    )
)

cd /d "%PROJECT_DIR%"
java -Dmaven.multiModuleProjectDirectory="%PROJECT_DIR%" -cp "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*