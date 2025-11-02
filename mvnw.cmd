@echo off
REM Lightweight mvnw shim for Windows - tries system mvn, then IntelliJ bundled mvn, otherwise prints instructions.
SETLOCAL

n:: Try system mvn first
where mvn >nul 2>&1
if %ERRORLEVEL%==0 (
    mvn %*
    exit /b %ERRORLEVEL%
)

n:: Try IntelliJ bundled Maven (common path used earlier)
SET "INTELLIJ_MVN=C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\plugins\maven\lib\maven3\bin\mvn.cmd"
if exist "%INTELLIJ_MVN%" (
    "%INTELLIJ_MVN%" %*
    exit /b %ERRORLEVEL%
)

necho No 'mvn' found in PATH and IntelliJ bundled mvn not found.
echo Please install Maven or run the build from IntelliJ.
echo To install Maven on Windows with Chocolatey: choco install maven
exit /b 1
