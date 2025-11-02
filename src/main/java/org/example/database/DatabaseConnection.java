package org.example.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton-Klasse für Datenbank-Verbindungsmanagement.
 * SOLID-Prinzip: Single Responsibility - verwaltet nur Datenbankverbindungen.
 */
public class DatabaseConnection {
    private static final String URL = "jdbc:postgresql://localhost:5432/mrp_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";

    // Retry-Logik: Versucht 30 mal mit 2 Sekunden Pause, falls DB noch nicht bereit ist
    // Wichtig für docker-compose, wo die DB nach dem Server startet
    private static final int RETRY_COUNT = 30;
    private static final int RETRY_DELAY_MS = 2000;

    // Singleton-Instanz (volatile für Thread-Safety)
    private static volatile DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() throws SQLException {
        // PostgreSQL JDBC-Treiber laden
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL Driver not found", e);
        }

        // Verbindung mit Retry-Logik aufbauen
        SQLException lastException = null;
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            try {
                this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
                initializeTables(); // DB-Schema erstellen falls nicht vorhanden
                lastException = null;
                break;
            } catch (SQLException e) {
                lastException = e;
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Interrupted while waiting for database to become available", ie);
                }
            }
        }

        if (lastException != null) {
            throw new SQLException("Unable to connect to PostgreSQL after multiple attempts", lastException);
        }
    }

    // Double-Checked Locking Pattern für Thread-sichere Singleton-Instanz
    // Prüft ob Instanz existiert und Verbindung noch offen ist
    public static DatabaseConnection getInstance() throws SQLException {
        if (instance == null || instance.connection == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null || instance.connection == null) {
                    instance = new DatabaseConnection();
                }
            }
        } else {
            // Prüfen ob Verbindung noch offen ist, sonst neu verbinden
            try {
                if (instance.connection.isClosed()) {
                    synchronized (DatabaseConnection.class) {
                        if (instance == null || instance.connection == null || instance.connection.isClosed()) {
                            instance = new DatabaseConnection();
                        }
                    }
                }
            } catch (SQLException e) {
                synchronized (DatabaseConnection.class) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    /**
     * Erstellt alle benötigten Tabellen für die Media Ratings Platform.
     * <p>
     * Wird beim ersten Start automatisch ausgeführt. Enthält die Tabellen:
     * <ul>
     * <li>users: Speichert alle User-Accounts mit Credentials und Token für Auth</li>
     * <li>media: Alle Film/Serien/Spiel-Einträge</li>
     * <li>ratings: User-Bewertungen für Media-Einträge</li>
     * <li>favorites: Many-to-Many Tabelle für User &lt;-&gt; Media Favoriten</li>
     * <li>rating_likes: Welcher User hat welches Rating geliked</li>
     * </ul>
     *
     * @throws SQLException wenn ein Fehler beim Erstellen der Tabellen auftritt
     */
    private void initializeTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Users-Tabelle: speichert Benutzerdaten und Credentials
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id SERIAL PRIMARY KEY,
                    username VARCHAR(255) UNIQUE NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    email VARCHAR(255),
                    favorite_genre VARCHAR(100),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    token VARCHAR(500)
                )
            """);

            // Email-Spalte hinzufügen falls noch nicht vorhanden (für Migration)
            stmt.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255)");

            // Media-Tabelle: Filme, Serien, Spiele
            // genres ist ein Array (TEXT[]) für mehrere Genres pro Medium
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS media (
                    id SERIAL PRIMARY KEY,
                    title VARCHAR(500) NOT NULL,
                    description TEXT,
                    media_type VARCHAR(50) NOT NULL,
                    release_year INTEGER,
                    genres TEXT[],
                    age_restriction INTEGER,
                    creator_id INTEGER REFERENCES users(id),
                    average_score DECIMAL(3,2) DEFAULT 0.0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Ratings-Tabelle: Benutzerbewertungen für Medien
            // UNIQUE(user_id, media_id) = ein User kann ein Medium nur einmal bewerten
            // confirmed=false bedeutet Kommentar muss erst vom Creator freigegeben werden
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ratings (
                    id SERIAL PRIMARY KEY,
                    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
                    media_id INTEGER REFERENCES media(id) ON DELETE CASCADE,
                    stars INTEGER CHECK (stars >= 1 AND stars <= 5),
                    comment TEXT,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    likes INTEGER DEFAULT 0,
                    confirmed BOOLEAN DEFAULT FALSE,
                    UNIQUE(user_id, media_id)
                )
            """);

            // Favorites-Tabelle: Many-to-Many Tabelle für User <-> Media Favoriten
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS favorites (
                    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
                    media_id INTEGER REFERENCES media(id) ON DELETE CASCADE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY(user_id, media_id)
                )
            """);

            // Rating-Likes-Tabelle: Benutzer können Ratings liken
            // Verhindert dass ein User dasselbe Rating mehrmals liken kann
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rating_likes (
                    rating_id INTEGER REFERENCES ratings(id) ON DELETE CASCADE,
                    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
                    PRIMARY KEY(rating_id, user_id)
                )
            """);
        }
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
