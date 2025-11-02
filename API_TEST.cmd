@echo off
setlocal EnableDelayedExpansion
REM ============================================================================
REM API TEST - Media Ratings Platform
REM ============================================================================

echo.
echo ========================================
echo   MRP - API TEST
echo ========================================
echo.
echo Server muss laufen auf http://localhost:8080
echo.

REM Server-Check
curl -s http://localhost:8080/ >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [FEHLER] Server nicht erreichbar!
    pause
    exit /b 1
)
echo [OK] Server erreichbar
echo.
pause

REM =============================================================================
REM TEST 1: USER REGISTRIERUNG
REM =============================================================================
:register
echo.
echo.
echo ========================================
echo TEST 1: USER REGISTRIERUNG
echo ========================================
echo.
set /p USERNAME="Username: "
set /p PASSWORD="Password: "

echo.
echo POST /api/users/register
(
echo {"username":"%USERNAME%","password":"%PASSWORD%"}
) > temp_register.json

curl -X POST http://localhost:8080/api/users/register -H "Content-Type: application/json" --data-binary @temp_register.json
echo.
del temp_register.json
pause

REM =============================================================================
REM TEST 2: USER LOGIN
REM =============================================================================
:login
echo.
echo.
echo ========================================
echo TEST 2: USER LOGIN
echo ========================================
echo.
echo POST /api/users/login

(
echo {"username":"%USERNAME%","password":"%PASSWORD%"}
) > temp_login.json

curl -s -X POST http://localhost:8080/api/users/login -H "Content-Type: application/json" --data-binary @temp_login.json > temp_login_response.json
type temp_login_response.json
echo.

REM Token extrahieren - einfache Methode ohne externes Script
for /f "tokens=2 delims=:}" %%a in (temp_login_response.json) do set TOKEN=%%a
set TOKEN=%TOKEN:"=%
set TOKEN=%TOKEN: =%

del temp_login.json
del temp_login_response.json

echo.
echo Token: !TOKEN!
echo.
pause

REM =============================================================================
REM TEST 3: MEDIA ERSTELLEN (MOVIE)
REM =============================================================================
:create_movie
echo.
echo.
echo ========================================
echo TEST 3: MEDIA ERSTELLEN (MOVIE)
echo ========================================
echo.
set /p MOVIE_TITLE="Titel: "
set /p MOVIE_DESC="Beschreibung: "
set /p MOVIE_GENRES="Genres: "

:movie_year_input
set /p MOVIE_YEAR="Jahr: "
echo %MOVIE_YEAR%| findstr /r "^[0-9][0-9]*$" >nul
if errorlevel 1 (
    echo [FEHLER] Bitte eine gueltige Jahreszahl eingeben!
    goto movie_year_input
)

:movie_age_input
set /p MOVIE_AGE="Altersfreigabe: "
echo %MOVIE_AGE%| findstr /r "^[0-9][0-9]*$" >nul
if errorlevel 1 (
    echo [FEHLER] Bitte eine gueltige Zahl eingeben!
    goto movie_age_input
)

echo.
echo POST /api/media

REM Genres in JSON-Array konvertieren
set GENRES_JSON=
for %%a in (%MOVIE_GENRES%) do (
    if defined GENRES_JSON (
        set GENRES_JSON=!GENRES_JSON!,"%%a"
    ) else (
        set GENRES_JSON="%%a"
    )
)

(
echo {"title":"%MOVIE_TITLE%","description":"%MOVIE_DESC%","mediaType":"movie","releaseYear":%MOVIE_YEAR%,"genres":[!GENRES_JSON!],"ageRestriction":%MOVIE_AGE%}
) > temp_movie.json

curl -X POST http://localhost:8080/api/media -H "Content-Type: application/json" -H "Authorization: Bearer !TOKEN!" --data-binary @temp_movie.json
echo.
del temp_movie.json
pause

REM =============================================================================
REM TEST 4: MEDIA ERSTELLEN (SERIES)
REM =============================================================================
:create_series
echo.
echo.
echo ========================================
echo TEST 4: MEDIA ERSTELLEN (SERIES)
echo ========================================
echo.
set /p SERIES_TITLE="Titel: "
set /p SERIES_DESC="Beschreibung: "
set /p SERIES_GENRES="Genres: "

:series_year_input
set /p SERIES_YEAR="Jahr: "
echo %SERIES_YEAR%| findstr /r "^[0-9][0-9]*$" >nul
if errorlevel 1 (
    echo [FEHLER] Bitte eine gueltige Jahreszahl eingeben!
    goto series_year_input
)

:series_age_input
set /p SERIES_AGE="Altersfreigabe: "
echo %SERIES_AGE%| findstr /r "^[0-9][0-9]*$" >nul
if errorlevel 1 (
    echo [FEHLER] Bitte eine gueltige Zahl eingeben!
    goto series_age_input
)

echo.
echo POST /api/media

REM Genres in JSON-Array konvertieren
set GENRES_JSON=
for %%a in (%SERIES_GENRES%) do (
    if defined GENRES_JSON (
        set GENRES_JSON=!GENRES_JSON!,"%%a"
    ) else (
        set GENRES_JSON="%%a"
    )
)

(
echo {"title":"%SERIES_TITLE%","description":"%SERIES_DESC%","mediaType":"series","releaseYear":%SERIES_YEAR%,"genres":[!GENRES_JSON!],"ageRestriction":%SERIES_AGE%}
) > temp_series.json

curl -X POST http://localhost:8080/api/media -H "Content-Type: application/json" -H "Authorization: Bearer !TOKEN!" --data-binary @temp_series.json
echo.
del temp_series.json
pause

REM =============================================================================
REM TEST 5: ALLE MEDIA ABRUFEN
REM =============================================================================
:get_all
echo.
echo.
echo ========================================
echo TEST 5: ALLE MEDIA ABRUFEN
echo ========================================
echo.
echo GET /api/media
echo.

curl -X GET http://localhost:8080/api/media -H "Authorization: Bearer !TOKEN!"
echo.
pause

REM =============================================================================
REM TEST 6: EINZELNE MEDIA ABRUFEN
REM =============================================================================
:get_one
echo.
echo.
echo ========================================
echo TEST 6: EINZELNE MEDIA ABRUFEN
echo ========================================
echo.
set /p MEDIA_ID="Media-ID: "

echo.
echo GET /api/media/%MEDIA_ID%
echo.

curl -X GET http://localhost:8080/api/media/%MEDIA_ID% -H "Authorization: Bearer !TOKEN!"
echo.
pause

REM =============================================================================
REM TEST 7: MEDIA AKTUALISIEREN
REM =============================================================================
:update
echo.
echo.
echo ========================================
echo TEST 7: MEDIA AKTUALISIEREN
echo ========================================
echo.
set /p UPDATE_ID="Media-ID: "

echo.
echo Lade aktuelle Daten...
echo.
echo VORHER:
curl -s -X GET http://localhost:8080/api/media/%UPDATE_ID% -H "Authorization: Bearer !TOKEN!"
echo.
echo.

set /p UPDATE_TITLE="Neuer Titel: "
set /p UPDATE_DESC="Neue Beschreibung: "
set /p UPDATE_GENRES="Neue Genres: "

echo.
echo PUT /api/media/%UPDATE_ID%

REM Genres in JSON-Array konvertieren
set GENRES_JSON=
for %%a in (%UPDATE_GENRES%) do (
    if defined GENRES_JSON (
        set GENRES_JSON=!GENRES_JSON!,"%%a"
    ) else (
        set GENRES_JSON="%%a"
    )
)

(
echo {"title":"%UPDATE_TITLE%","description":"%UPDATE_DESC%","mediaType":"movie","releaseYear":2010,"genres":[!GENRES_JSON!],"ageRestriction":12}
) > temp_update.json

echo.
curl -X PUT http://localhost:8080/api/media/%UPDATE_ID% -H "Content-Type: application/json" -H "Authorization: Bearer !TOKEN!" --data-binary @temp_update.json
echo.
del temp_update.json

echo.
echo NACHHER:
curl -s -X GET http://localhost:8080/api/media/%UPDATE_ID% -H "Authorization: Bearer !TOKEN!"
echo.
echo.
pause

REM =============================================================================
REM TEST 8: MEDIA LOESCHEN
REM =============================================================================
:delete
echo.
echo.
echo ========================================
echo TEST 8: MEDIA LOESCHEN
echo ========================================
echo.
set /p DELETE_ID="Media-ID: "
set /p CONFIRM="Wirklich loeschen? (j/n): "

if /i not "%CONFIRM%"=="j" (
    echo Abgebrochen.
    pause
    goto :menu
)

echo.
echo DELETE /api/media/%DELETE_ID%
echo.

curl -X DELETE http://localhost:8080/api/media/%DELETE_ID% -H "Authorization: Bearer !TOKEN!"
echo.
echo.
pause

REM =============================================================================
REM MENU
REM =============================================================================
:menu
echo.
echo.
echo ========================================
echo   MENU
echo ========================================
echo.
echo [1] User Registrierung
echo [2] User Login
echo [3] Movie erstellen
echo [4] Series erstellen
echo [5] Alle Media abrufen
echo [6] Eine Media abrufen
echo [7] Media aktualisieren
echo [8] Media loeschen
echo.
echo [Q] Beenden
echo.
set /p CHOICE="Wahl: "

if /i "%CHOICE%"=="Q" goto :end
if "%CHOICE%"=="1" goto :register
if "%CHOICE%"=="2" goto :login
if "%CHOICE%"=="3" goto :create_movie
if "%CHOICE%"=="4" goto :create_series
if "%CHOICE%"=="5" goto :get_all
if "%CHOICE%"=="6" goto :get_one
if "%CHOICE%"=="7" goto :update
if "%CHOICE%"=="8" goto :delete

echo [FEHLER] Ungueltige Auswahl!
goto :menu

:end
echo.
pause
