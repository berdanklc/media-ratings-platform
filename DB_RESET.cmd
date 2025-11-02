@echo off
REM ============================================================================
REM Datenbank zuruecksetzen - Loescht alle User und Media
REM ============================================================================

echo.
echo ========================================
echo   DATENBANK ZURUECKSETZEN
echo ========================================
echo.
echo WARNUNG: Dies loescht ALLE Daten aus der Datenbank!
echo - Alle User werden geloescht
echo - Alle Media-Eintraege werden geloescht
echo - Alle Ratings werden geloescht
echo.
set /p CONFIRM="Wirklich fortfahren? (j/n): "

if /i not "%CONFIRM%"=="j" (
    echo Abgebrochen.
    pause
    exit /b 0
)

echo.
echo Loesche Daten...
echo.

REM PostgreSQL Befehle ausfuehren
docker exec -i mrp_postgres psql -U postgres -d mrp_db -c "DELETE FROM rating_likes;"
docker exec -i mrp_postgres psql -U postgres -d mrp_db -c "DELETE FROM ratings;"
docker exec -i mrp_postgres psql -U postgres -d mrp_db -c "DELETE FROM favorites;"
docker exec -i mrp_postgres psql -U postgres -d mrp_db -c "DELETE FROM media;"
docker exec -i mrp_postgres psql -U postgres -d mrp_db -c "DELETE FROM users;"

REM ID-Sequenzen zurücksetzen
docker exec -i mrp_postgres psql -U postgres -d mrp_db -c "ALTER SEQUENCE users_id_seq RESTART WITH 1;"
docker exec -i mrp_postgres psql -U postgres -d mrp_db -c "ALTER SEQUENCE media_id_seq RESTART WITH 1;"
docker exec -i mrp_postgres psql -U postgres -d mrp_db -c "ALTER SEQUENCE ratings_id_seq RESTART WITH 1;"

echo.
echo ========================================
echo   FERTIG!
echo ========================================
echo.
echo Alle Daten wurden geloescht.
echo Die Datenbank ist jetzt leer.
echo.
echo Naechster User bekommt ID 1
echo Naechster Media-Eintrag bekommt ID 1
echo.
pause
