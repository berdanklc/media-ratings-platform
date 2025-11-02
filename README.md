# Media Ratings Platform (MRP)

**GitHub Repository:** https://github.com/berdanklc/media-ratings-platform

Standalone REST-Server in Java 21 gemäß Aufgabenstellung (kein Web-Framework). Implementiert User-Registrierung/Login mit token-basierter Autorisierung und vollständige Media-Verwaltung (CRUD). Das Frontend ist nicht Teil dieses Projekts.

## Inhalt
- Übersicht und Tech-Stack
- Voraussetzungen
- Setup und Installation
- Spezifikation
- API-Dokumentation mit curl-Beispielen
- HTTP-Statuscodes
- Tests
- Projektstruktur
- Troubleshooting
- Autor & Repository

## Übersicht und Tech-Stack

Das Projekt implementiert einen RESTful HTTP-Server für eine Media-Bewertungsplattform.

**Technologien:**
- **Sprache:** Java 21
- **HTTP-Server:** com.sun.net.httpserver.HttpServer (pure HTTP, kein Framework)
- **Datenbank:** PostgreSQL (läuft in Docker)
- **Datenbankzugriff:** JDBC mit PreparedStatements
- **JSON-Serialisierung:** Jackson (mit jackson-datatype-jsr310)
- **Build-Tool:** Maven mit Maven Wrapper
- **Testing:** JUnit 5 und Mockito

## Voraussetzungen

- Windows mit cmd.exe (alle Beispiele sind für Windows formatiert)
- Java Development Kit (JDK) 21 oder höher im PATH
- Docker Desktop (für PostgreSQL)
- curl (für API-Tests)

## Setup und Installation

### 1. Repository klonen

```cmd
git clone https://github.com/berdanklc/media-ratings-platform.git
cd media-ratings-platform
```

### 2. PostgreSQL-Datenbank starten

```cmd
docker-compose up -d
```

**Überprüfung:**
```cmd
docker ps
```
Sie sollten einen laufenden Container sehen (z.B. "mrp-postgres").

**Datenbank-Parameter:**
- Host: localhost:5432
- Datenbank: mrp_db
- Benutzer: mrp_user
- Passwort: mrp_password

### 3. Server kompilieren und starten

```cmd
mvnw.cmd clean compile exec:java
```

**Erwartete Ausgabe:**
```
Database connection established successfully
Server successfully started on port 8080
```

Der Server ist jetzt unter **http://localhost:8080** erreichbar.

## Spezifikation

Die Media Ratings Platform ermöglicht es Benutzern:
- Sich zu registrieren und anzumelden
- Media-Einträge (Filme, Serien, Spiele) zu erstellen, anzusehen, zu bearbeiten und zu löschen
- Token-basierte Authentifizierung für geschützte Endpunkte zu nutzen
- Nur eigene Media-Einträge zu bearbeiten oder zu löschen

### Sicherheit
Alle geschützten Endpunkte erfordern einen gültigen Token im Authorization-Header:
```
Authorization: Bearer <token>
```

## API-Dokumentation

Alle Beispiele verwenden curl für Windows cmd.exe.

### 1. Benutzerregistrierung

Registriert einen neuen Benutzer.

```cmd
curl -X POST http://localhost:8080/api/users/register ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"testuser\",\"password\":\"test123\"}"
```

**Erfolgreiche Antwort (201 Created):**
```json
{
  "id": 1,
  "username": "testuser",
  "createdAt": "2025-11-02T15:30:00.123456"
}
```

### 2. Benutzer-Login

Authentifiziert einen Benutzer und gibt ein Token zurück.

```cmd
curl -X POST http://localhost:8080/api/users/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"testuser\",\"password\":\"test123\"}"
```

**Erfolgreiche Antwort (200 OK):**
```json
{
  "token": "testuser-mrpToken-c2182eeb-418b-4fec-b637-ea235775b0cb"
}
```

**Wichtig:** Speichern Sie den Token für nachfolgende Requests.

### 3. Media-Eintrag erstellen

Erstellt einen neuen Media-Eintrag (erfordert Authentifizierung).

```cmd
curl -X POST http://localhost:8080/api/media ^
  -H "Authorization: Bearer testuser-mrpToken-c2182eeb-418b-4fec-b637-ea235775b0cb" ^
  -H "Content-Type: application/json" ^
  -d "{\"title\":\"Inception\",\"description\":\"Sci-fi thriller about dreams\",\"mediaType\":\"movie\",\"releaseYear\":2010,\"genres\":[\"sci-fi\",\"thriller\"],\"ageRestriction\":12}"
```

**Erfolgreiche Antwort (201 Created):**
```json
{
  "id": 1,
  "title": "Inception",
  "description": "Sci-fi thriller about dreams",
  "mediaType": "movie",
  "releaseYear": 2010,
  "genres": ["sci-fi", "thriller"],
  "ageRestriction": 12,
  "creatorId": 1,
  "createdAt": "2025-11-02T15:35:00.123456"
}
```

### 4. Alle Media-Einträge abrufen

Gibt eine Liste aller Media-Einträge zurück (erfordert Authentifizierung).

```cmd
curl -X GET http://localhost:8080/api/media ^
  -H "Authorization: Bearer testuser-mrpToken-c2182eeb-418b-4fec-b637-ea235775b0cb"
```

**Erfolgreiche Antwort (200 OK):**
```json
[
  {
    "id": 1,
    "title": "Inception",
    "description": "Sci-fi thriller about dreams",
    "mediaType": "movie",
    "releaseYear": 2010,
    "genres": ["sci-fi", "thriller"],
    "ageRestriction": 12,
    "creatorId": 1,
    "createdAt": "2025-11-02T15:35:00.123456"
  }
]
```

### 5. Einzelnen Media-Eintrag abrufen

Ruft einen spezifischen Media-Eintrag ab (erfordert Authentifizierung).

```cmd
curl -X GET http://localhost:8080/api/media/1 ^
  -H "Authorization: Bearer testuser-mrpToken-c2182eeb-418b-4fec-b637-ea235775b0cb"
```

**Erfolgreiche Antwort (200 OK):** Gibt das Media-Objekt zurück.

### 6. Media-Eintrag aktualisieren

Aktualisiert einen Media-Eintrag. Nur der Ersteller darf dies tun.

```cmd
curl -X PUT http://localhost:8080/api/media/1 ^
  -H "Authorization: Bearer testuser-mrpToken-c2182eeb-418b-4fec-b637-ea235775b0cb" ^
  -H "Content-Type: application/json" ^
  -d "{\"title\":\"Inception Updated\",\"description\":\"Updated description\",\"mediaType\":\"movie\",\"releaseYear\":2010,\"genres\":[\"sci-fi\"],\"ageRestriction\":16}"
```

**Erfolgreiche Antwort (200 OK):** Gibt das aktualisierte Media-Objekt zurück.

**Fehler bei fehlender Berechtigung (403 Forbidden):**
```json
{
  "error": "Forbidden: Only the creator can modify this media entry."
}
```

### 7. Media-Eintrag löschen

Löscht einen Media-Eintrag. Nur der Ersteller darf dies tun.

```cmd
curl -X DELETE http://localhost:8080/api/media/1 ^
  -H "Authorization: Bearer testuser-mrpToken-c2182eeb-418b-4fec-b637-ea235775b0cb"
```

**Erfolgreiche Antwort (204 No Content):** Keine Antwort-Body.

## HTTP-Statuscodes

Die API verwendet standardkonforme HTTP-Statuscodes:

**Erfolgreiche Anfragen:**
- **200 OK** - Anfrage erfolgreich (Login, Read, Update)
- **201 Created** - Ressource erfolgreich erstellt (Register, Media erstellt)
- **204 No Content** - Erfolgreich gelöscht

**Client-Fehler:**
- **400 Bad Request** - Ungültige Eingabedaten
- **401 Unauthorized** - Token fehlt oder ist ungültig
- **403 Forbidden** - Keine Berechtigung für diese Aktion
- **404 Not Found** - Ressource nicht gefunden
- **405 Method Not Allowed** - HTTP-Methode nicht erlaubt

**Server-Fehler:**
- **500 Internal Server Error** - Serverfehler (z.B. Datenbankverbindung fehlgeschlagen)

## Tests

### Unit-Tests ausführen

```cmd
mvnw.cmd test
```

Die Unit-Tests validieren die Kernlogik:
- **ModelClassesTest** - Testet Model-Klassen (User, MediaEntry, Rating)
- **AuthServiceTest** - Testet Authentifizierungslogik (Register, Login, Token-Validierung)
- **MediaServiceTest** - Testet Media-Verwaltungslogik (CRUD, Autorisierung)

Test-Reports werden in `target/surefire-reports/` gespeichert.

### Integrationstests mit curl-Script

Ein automatisiertes Test-Script steht zur Verfügung:

```cmd
API_TEST.cmd
```

Dieses Script führt Sie interaktiv durch alle API-Endpunkte.

### Postman-Collection

Alternativ können Sie die mitgelieferte Postman-Collection verwenden:
1. Öffnen Sie Postman
2. Importieren Sie `MRP_Postman_Collection.json`
3. Führen Sie die Requests in der Reihenfolge aus

## Projektstruktur

```
src/main/java/org/example/
├── Main.java                    # Einstiegspunkt der Anwendung
├── controller/
│   ├── AuthController.java      # Handling für /api/users/*
│   └── MediaController.java     # Handling für /api/media/*
├── service/
│   ├── AuthService.java         # Authentifizierungs-Business-Logik
│   └── MediaService.java        # Media-Verwaltungs-Business-Logik
├── repository/
│   ├── UserRepository.java      # Datenbankzugriff für User
│   ├── MediaRepository.java     # Datenbankzugriff für Media
│   └── RatingRepository.java    # Datenbankzugriff für Ratings
├── model/
│   ├── User.java               # User-Model
│   ├── MediaEntry.java         # Media-Model
│   └── Rating.java             # Rating-Model
├── server/
│   └── MRPServer.java          # HTTP-Server und Routing
├── database/
│   └── DatabaseConnection.java # Datenbank-Verbindungsmanagement
└── exception/
    └── ForbiddenException.java # Custom Exception für 403-Fehler
```

**Weitere Dateien im Projekt-Root:**
- `docker-compose.yml` - PostgreSQL-Container-Konfiguration
- `pom.xml` - Maven-Abhängigkeiten und Build-Konfiguration
- `API_TEST.cmd` - Automatisiertes Test-Script
- `DB_RESET.cmd` - Script zum Zurücksetzen der Datenbank
- `MRP_Postman_Collection.json` - Postman-Collection für API-Tests

## Troubleshooting

### Datenbankverbindung schlägt fehl

**Problem:** `Connection refused` oder ähnliche Fehler

**Lösung:**
```cmd
docker ps
docker-compose down
docker-compose up -d
```
Warten Sie einige Sekunden und starten Sie dann den Server neu:
```cmd
mvnw.cmd clean compile exec:java
```

### Port 8080 ist bereits belegt

**Problem:** `Address already in use`

**Lösung - Port-Nutzung prüfen:**
```cmd
netstat -ano | findstr :8080
```

**Prozess beenden:**
```cmd
taskkill /PID <PID> /F
```

### PostgreSQL-Port 5432 ist belegt

**Lösung:** Ändern Sie den Port in `docker-compose.yml`:
```yaml
ports:
  - "5433:5432"  # Host-Port auf 5433 ändern
```

Passen Sie auch `DatabaseConnection.java` entsprechend an.

### Datenbank zurücksetzen

Falls Sie die Datenbank komplett zurücksetzen möchten:

```cmd
DB_RESET.cmd
```

## Autor & Repository

- **Autor:** Berdan Kilic
- **GitHub:** https://github.com/berdanklc/media-ratings-platform
