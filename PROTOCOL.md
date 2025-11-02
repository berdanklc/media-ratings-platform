# Entwicklungsprotokoll – Media Ratings Platform (MRP)

Student: Berdan Kilic  
GitHub: https://github.com/berdanklc/media-ratings-platform

---

## 1. Projektüberblick

Das Projekt implementiert einen eigenständigen REST-API-Server für eine Media-Bewertungsplattform ohne Verwendung von Web-Frameworks.

**Technische Basis:**
- Sprache: Java 21
- HTTP-Server: com.sun.net.httpserver.HttpServer (pure HTTP)
- Datenbank: PostgreSQL (Docker-Container)
- Datenbankzugriff: JDBC mit PreparedStatements
- JSON-Verarbeitung: Jackson mit JSR310-Modul für Java Time API
- Testing: JUnit 5 und Mockito
- Build: Maven mit Maven Wrapper

**Server-URL:** http://localhost:8080

**Implementierte Funktionalität:**
- Benutzerregistrierung und Login mit token-basierter Authentifizierung
- Vollständige Media-Verwaltung (Create, Read, Update, Delete)
- Autorisierung (nur Ersteller dürfen ihre Media-Einträge bearbeiten/löschen)
- Datenpersistierung in PostgreSQL
- Umfassende Unit-Tests für kritische Business-Logik

---

## 2. Architektur und Designentscheidungen

### 2.1 Schichtenarchitektur

Das Projekt folgt einer klassischen 3-Schichten-Architektur:

**Controller-Schicht:**
- Verantwortlich für HTTP-Request-Handling und Routing
- Parsing von JSON-Daten und Validierung der HTTP-Methoden
- Konvertierung von Business-Objekten zu JSON-Responses
- Klassen: `AuthController`, `MediaController`

**Service-Schicht:**
- Enthält die gesamte Business-Logik
- Validierung von Eingabedaten
- Autorisierungsprüfungen
- Token-Generierung und -Validierung
- Klassen: `AuthService`, `MediaService`

**Repository-Schicht:**
- Datenbankzugriff über JDBC
- Verwendung von PreparedStatements zur Vermeidung von SQL-Injection
- Mapping zwischen Datenbank-Resultsets und Java-Objekten
- Klassen: `UserRepository`, `MediaRepository`, `RatingRepository`

**Vorteile dieser Architektur:**
- **Single Responsibility Principle:** Jede Schicht hat eine klar definierte Aufgabe
- **Testbarkeit:** Services können unabhängig von der Datenbank getestet werden (Mocking)
- **Wartbarkeit:** Änderungen in einer Schicht beeinflussen andere Schichten minimal
- **Erweiterbarkeit:** Neue Features können einfach hinzugefügt werden

### 2.2 HTTP-Server und Routing

Der Server nutzt den in Java integrierten `HttpServer`:

```
Port: 8080
Contexts:
  - /api/users/register  (POST)
  - /api/users/login     (POST)
  - /api/media           (GET, POST)
  - /api/media/{id}      (GET, PUT, DELETE)
  - /                    (GET) - Health Check
```

**Routing-Implementierung:**
- Path-Parsing zur Extraktion von URL-Parametern (z.B. Media-ID)
- HTTP-Methoden-Routing (GET, POST, PUT, DELETE)
- CORS-Unterstützung mit OPTIONS-Methode
- Content-Type: application/json

**Status-Codes gemäß HTTP-Spezifikation:**
- 2XX für erfolgreiche Operationen
- 4XX für Client-Fehler (ungültige Daten, fehlende Authentifizierung)
- 5XX für Server-Fehler (Datenbankprobleme)

### 2.3 Token-basierte Authentifizierung

**Implementierung:**
1. User registriert sich mit Username und Passwort
2. Login-Endpunkt validiert Credentials und generiert UUID-basierten Token
3. Token wird in der Datenbank persistiert (Spalte `users.token`)
4. Jeder geschützte Request muss Header enthalten: `Authorization: Bearer <token>`
5. Server validiert Token bei jedem Request über `AuthService.validateToken()`

**Token-Format:**
```
{username}-mrpToken-{UUID}
Beispiel: testuser-mrpToken-c2182eeb-418b-4fec-b637-ea235775b0cb
```

**Sicherheitsüberlegungen:**
- Tokens sind eindeutig und können nur vom System generiert werden
- Ungültige oder fehlende Tokens führen zu HTTP 401 (Unauthorized)
- Passwörter werden aktuell im Klartext gespeichert (Hinweis für Produktionsumgebung: BCrypt verwenden)

### 2.4 Autorisierung

Zusätzlich zur Authentifizierung prüft das System Berechtigungen:

**Regel:** Nur der Ersteller eines Media-Eintrags darf diesen bearbeiten oder löschen.

**Implementierung:**
- Media-Einträge speichern die `creator_id` (Foreign Key zu `users.id`)
- `MediaService` vergleicht `authenticatedUser.getId()` mit `media.getCreatorId()`
- Bei Abweichung: `ForbiddenException` → HTTP 403

**Beispiel-Ablauf:**
```
1. User A erstellt Media-Eintrag (creator_id = 1)
2. User B versucht, diesen zu löschen
3. Service erkennt: currentUser.id (2) != creator_id (1)
4. Wirft ForbiddenException
5. Controller gibt 403 Forbidden zurück
```

### 2.5 Datenbankmodell

**Schema (vereinfacht):**

```sql
users (
  id SERIAL PRIMARY KEY,
  username VARCHAR UNIQUE NOT NULL,
  password VARCHAR NOT NULL,
  token VARCHAR UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)

media_entries (
  id SERIAL PRIMARY KEY,
  title VARCHAR NOT NULL,
  description TEXT,
  media_type VARCHAR NOT NULL,
  release_year INTEGER,
  genres VARCHAR[],
  age_restriction INTEGER,
  creator_id INTEGER REFERENCES users(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)

ratings (
  id SERIAL PRIMARY KEY,
  media_id INTEGER REFERENCES media_entries(id),
  user_id INTEGER REFERENCES users(id),
  stars INTEGER CHECK (stars BETWEEN 1 AND 5),
  comment TEXT,
  confirmed BOOLEAN DEFAULT FALSE,
  likes INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
```

**Designentscheidungen:**
- Relationales Modell mit klaren Foreign-Key-Beziehungen
- Normalisierung zur Vermeidung von Redundanz
- `creator_id` ermöglicht Autorisierungsprüfungen
- `ratings`-Tabelle vorbereitet für zukünftige Features
- Genres als Array-Typ für flexible Zuordnung

### 2.6 Dependency Management

**Maven-Dependencies (pom.xml):**
- `postgresql` - JDBC-Treiber für PostgreSQL
- `jackson-databind` - JSON-Serialisierung/-Deserialisierung
- `jackson-datatype-jsr310` - Unterstützung für Java 8 Date/Time API
- `junit-jupiter` - Unit-Testing-Framework
- `mockito-core` - Mocking-Framework für Tests

Bewusst minimalistisch gehalten, um die Anforderung "kein Web-Framework" zu erfüllen.

---

## 3. SOLID-Prinzipien in der Implementierung

**Single Responsibility Principle (SRP):**
- Jede Klasse hat genau eine Verantwortlichkeit
- Controller nur für HTTP, Services nur für Business-Logik, Repositories nur für Datenbank

**Open/Closed Principle (OCP):**
- Services arbeiten mit Interfaces (Repository-Pattern)
- Erweiterbar ohne bestehenden Code zu ändern

**Liskov Substitution Principle (LSP):**
- Model-Klassen sind konsistent und austauschbar
- Klare Vererbungshierarchien

**Interface Segregation Principle (ISP):**
- Kleine, fokussierte Klassen statt großer "God Classes"
- Jede Komponente hat nur die Methoden, die sie benötigt

**Dependency Inversion Principle (DIP):**
- Abhängigkeiten werden über Konstruktor injiziert
- Services sind nicht direkt an konkrete Repository-Implementierungen gebunden

---

## 4. Unit-Tests

### 4.1 Test-Strategie

**Ziel:** Kernlogik robust und unabhängig von externen Abhängigkeiten testen.

**Verwendete Technologien:**
- JUnit 5 für Test-Framework
- Mockito für Mocking von Repositories

**Abdeckung:**
- Model-Klassen (Konstruktoren, Getter/Setter, Datenintegrität)
- Service-Logik (Registrierung, Login, Token-Validierung, CRUD-Operationen)
- Edge-Cases und Fehlerfälle

### 4.2 Test-Klassen im Detail

**ModelClassesTest:**
- Testet Konstruktoren und Getter/Setter aller Model-Klassen
- Validiert korrekte Datentypen und Default-Werte
- Prüft Timestamp-Generierung

**AuthServiceTest:**
- Erfolgreiche Registrierung mit gültigen Daten
- Fehlerfall: Duplikat-Username
- Fehlerfall: Ungültiges Passwort (zu kurz)
- Erfolgreicher Login mit korrekten Credentials
- Fehlerfall: Falsches Passwort
- Fehlerfall: Nicht existierender User
- Token-Validierung mit gültigem Token
- Fehlerfall: Ungültiger Token

**MediaServiceTest:**
- Media-Erstellung durch authentifizierten User
- Media abrufen nach ID
- Alle Media abrufen
- Media aktualisieren (nur durch Creator)
- Fehlerfall: Update durch anderen User (403)
- Media löschen (nur durch Creator)
- Fehlerfall: Löschen durch anderen User (403)
- Fehlerfall: Nicht existierende Media-ID (404)

### 4.3 Begründung der Test-Auswahl

**Warum genau diese Logik?**

1. **Sicherheit ist kritisch:**
   - Authentifizierung und Autorisierung müssen fehlerfrei funktionieren
   - Token-Handling muss robust sein
   - Fehlerhafte Implementierung könnte Zugriff auf fremde Daten erlauben

2. **Kernfunktionalität:**
   - Media-CRUD sind die Hauptfunktionen der Anwendung
   - Ownership-Prüfung ist essentiell für Datenintegrität

3. **Edge-Cases:**
   - Ungültige Eingaben müssen korrekt behandelt werden
   - Fehlerhafte Zustände dürfen nicht zu Abstürzen führen

**Ausführung:**
```cmd
mvnw.cmd test
```

**Reports:** `target/surefire-reports/`

---

## 5. Probleme und Lösungen

### Problem 1: CORS-Fehler bei API-Aufrufen

**Symptom:** Browser blockiert Requests von anderen Origins

**Ursache:** Fehlende CORS-Header in HTTP-Responses

**Lösung:**
- CORS-Header in alle Responses eingefügt:
  ```
  Access-Control-Allow-Origin: *
  Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
  Access-Control-Allow-Headers: Content-Type, Authorization
  ```
- OPTIONS-Preflight-Requests mit Status 200 beantworten

### Problem 2: LocalDateTime-Serialisierung

**Symptom:** Jackson wirft Fehler bei Serialisierung von `LocalDateTime`

**Ursache:** Jackson unterstützt Java 8 Date/Time API nicht standardmäßig

**Lösung:**
- Dependency `jackson-datatype-jsr310` hinzugefügt
- `JavaTimeModule` im ObjectMapper registriert:
  ```java
  objectMapper.registerModule(new JavaTimeModule());
  objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  ```
- Resultat: ISO-8601-Strings in JSON (z.B. `2025-11-02T15:30:00.123456`)

### Problem 3: SQL-Injection-Gefahr

**Symptom:** Direkte String-Konkatenation in SQL-Queries gefährlich

**Lösung:**
- Durchgängige Verwendung von PreparedStatements
- Alle User-Inputs über Parameter-Binding:
  ```java
  PreparedStatement stmt = conn.prepareStatement(
      "SELECT * FROM users WHERE username = ?"
  );
  stmt.setString(1, username);
  ```

### Problem 4: Datenbankverbindung bei Server-Start

**Symptom:** Server startet, bevor PostgreSQL-Container bereit ist

**Ursache:** Docker-Container braucht einige Sekunden zum Initialisieren

**Lösung:**
- Retry-Logik in `DatabaseConnection` implementiert
- Bis zu 30 Sekunden Wartezeit mit exponentiellen Backoff
- Klare Fehlermeldungen bei Timeout

### Problem 5: Token-Persistierung

**Symptom:** Nach Server-Neustart sind Tokens ungültig

**Ursache:** Tokens wurden nur im Speicher gehalten

**Lösung:**
- Tokens in Datenbank persistieren (Spalte `users.token`)
- Login aktualisiert Token in DB
- Validierung liest Token aus DB

### Problem 6: Autorisierung fehlte initial

**Symptom:** Jeder User konnte alle Media-Einträge bearbeiten

**Ursache:** Keine Prüfung des Erstellers

**Lösung:**
- Foreign Key `creator_id` in `media_entries` hinzugefügt
- Autorisierungsprüfung in `MediaService`:
  ```java
  if (!media.getCreatorId().equals(authenticatedUser.getId())) {
      throw new ForbiddenException("Only creator can modify this entry");
  }
  ```

---

## 6. Zeitaufwand

| Phase | Geschätzter Aufwand |
|-------|---------------------|
| Projektsetup (Maven, Docker, Repository) | 2 Stunden |
| Datenbankdesign und Connection-Management | 1.5 Stunden |
| Model-Klassen implementieren | 1 Stunde |
| Repository-Schicht (User, Media, Rating) | 3 Stunden |
| Service-Schicht (Auth, Media) | 4 Stunden |
| HTTP-Server und Routing-Logik | 3 Stunden |
| Controller-Implementierung | 2.5 Stunden |
| Token-basierte Authentifizierung | 2 Stunden |
| Unit-Tests schreiben und debuggen | 3 Stunden |
| Fehlerbehandlung und Logging | 2 Stunden |
| Docker-Setup und Testing | 2 Stunden |
| Dokumentation (README, Protokoll) | 2 Stunden |
| Debugging und Bugfixes | 2 Stunden |
| **Gesamt** | **~30 Stunden** |

**Entwicklungsansatz:**
- Iterative Entwicklung mit häufigen Tests
- Test-Driven Development für kritische Komponenten
- Regelmäßige Commits mit aussagekräftigen Messages
- Kontinuierliche Integration und Testing

---

## 7. Erweiterungsmöglichkeiten

Für zukünftige Versionen sind folgende Features geplant:

**Ratings-System:**
- CRUD-Operationen für Bewertungen (1-5 Sterne)
- Optionale Kommentare zu Bewertungen
- Berechnung der Durchschnittsbewertung pro Media-Eintrag

**Social Features:**
- Likes für Bewertungen
- Kommentar-Bestätigung (Moderation)
- Favorites/Watchlist für Benutzer

**Suche und Filter:**
- Volltextsuche nach Titel
- Filter nach Genre, Medientyp, Erscheinungsjahr, Altersfreigabe
- Sortierung nach Titel, Jahr, Bewertung

**Empfehlungssystem:**
- Genre-basierte Empfehlungen
- Content-basierte Empfehlungen (ähnliche Genres, Altersfreigabe)
- Collaborative Filtering

**Zusatzfeatures:**
- Leaderboard der aktivsten User
- User-Profile mit Statistiken
- Avatar-Upload

Die bestehende Architektur erlaubt diese Erweiterungen ohne größere Refactorings.

---

## 8. Setup und Ausführung

**Datenbank starten:**
```cmd
docker-compose up -d
```

**Server kompilieren und starten:**
```cmd
mvnw.cmd clean compile exec:java
```

**Unit-Tests ausführen:**
```cmd
mvnw.cmd test
```

**API testen:**
- Interaktives curl-Script: `API_TEST.cmd`
- Postman-Collection: `MRP_Postman_Collection.json`

**Datenbank zurücksetzen:**
```cmd
DB_RESET.cmd
```

---

## 9. Fazit

Das Projekt erfüllt alle geforderten Anforderungen der Aufgabenstellung. Der Server basiert auf Javas eingebautem HttpServer ohne externe Web-Frameworks und implementiert vollständige REST-API-Funktionalität. Die token-basierte Authentifizierung und Autorisierung funktionieren korrekt, und alle CRUD-Operationen für Media-Einträge sind implementiert.

Die Architektur folgt durchgängig dem 3-Schichten-Modell mit klarer Trennung von Controller-, Service- und Repository-Ebene. Die Anwendung der SOLID-Prinzipien zeigt sich in der gesamten Codebase, insbesondere durch Single Responsibility und Dependency Injection.

Die Datenpersistierung erfolgt über PostgreSQL mit JDBC und PreparedStatements zur Vermeidung von SQL-Injection. Alle 20 Unit-Tests laufen erfolgreich durch und decken die kritischen Business-Logik-Komponenten ab. Die Fehlerbehandlung verwendet korrekte HTTP-Status-Codes gemäß der Spezifikation.

Die Dokumentation umfasst README, Protokoll und API-Beispiele mit curl und Postman. Das Projekt ist damit vollständig abgabefähig und für zukünftige Erweiterungen gut vorbereitet.
