package org.example.repository;

import org.example.database.DatabaseConnection;
import org.example.model.MediaEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// Repository für Media-Tabelle
// Alle Datenbankoperationen für Media-Einträge (CRUD)
public class MediaRepository {
    private final Connection connection;

    public MediaRepository() throws SQLException {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    // Neuen MediaEntry in DB speichern
    // RETURNING gibt die automatisch generierte ID und created_at zurück
    public MediaEntry save(MediaEntry media) throws SQLException {
        String sql = """
            INSERT INTO media (title, description, media_type, release_year, genres, age_restriction, creator_id, average_score)
            VALUES (?, ?, ?, ?, ?, ?, ?, 0) RETURNING id, created_at
            """;

        List<String> genres = media.getGenres() != null ? media.getGenres() : Collections.emptyList();
        Array genresArray = null;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, media.getTitle());
            stmt.setString(2, media.getDescription());
            stmt.setString(3, media.getMediaType());
            stmt.setInt(4, media.getReleaseYear());
            // PostgreSQL Array erstellen für TEXT[] Spalte
            // Java List<String> wird zu PostgreSQL TEXT[]
            genresArray = connection.createArrayOf("text", genres.toArray(new String[0]));
            stmt.setArray(5, genresArray);
            stmt.setInt(6, media.getAgeRestriction());
            stmt.setInt(7, media.getCreatorId());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // ID und Timestamp von DB zurückholen
                    media.setId(rs.getInt("id"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        media.setCreatedAt(ts.toLocalDateTime());
                    }
                }
            }
            return media;
        } finally {
            // PostgreSQL Array manuell freigeben (Memory-Leak vermeiden)
            if (genresArray != null) {
                try {
                    genresArray.free();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public Optional<MediaEntry> findById(Integer id) throws SQLException {
        String sql = "SELECT * FROM media WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToMedia(rs));
                }
            }
            return Optional.empty();
        }
    }

    // Alle Media-Einträge holen (sortiert nach ID)
    public List<MediaEntry> findAll() throws SQLException {
        List<MediaEntry> mediaList = new ArrayList<>();
        String sql = "SELECT * FROM media ORDER BY id ASC";
        try (Statement stmt = connection.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    mediaList.add(mapRowToMedia(rs));
                }
            }
        }
        return mediaList;
    }

    // Bestehenden MediaEntry updaten (alle Felder)
    public void update(MediaEntry media) throws SQLException {
        String sql = "UPDATE media SET " +
                "title = ?, description = ?, media_type = ?, release_year = ?, " +
                "genres = ?, age_restriction = ?, average_score = ? " +
                "WHERE id = ?";

        List<String> genres = media.getGenres() != null ? media.getGenres() : Collections.emptyList();
        Array genresArray = null;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, media.getTitle());
            stmt.setString(2, media.getDescription());
            stmt.setString(3, media.getMediaType());
            stmt.setInt(4, media.getReleaseYear());
            genresArray = connection.createArrayOf("text", genres.toArray(new String[0]));
            stmt.setArray(5, genresArray);
            stmt.setInt(6, media.getAgeRestriction());
            stmt.setDouble(7, media.getAverageRating());
            stmt.setInt(8, media.getId());
            stmt.executeUpdate();
        } finally {
            if (genresArray != null) {
                try {
                    genresArray.free();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    // MediaEntry und alle zugehörigen Ratings löschen
    // Ratings zuerst wegen Foreign Key Constraint
    public void delete(Integer id) throws SQLException {
        // Erst alle Ratings zu diesem Media löschen
        String deleteRatingsSql = "DELETE FROM ratings WHERE media_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteRatingsSql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }

        // Dann Media-Eintrag selbst löschen
        String deleteMediaSql = "DELETE FROM media WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteMediaSql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // Konvertiert eine Datenbank-Zeile (ResultSet) zu MediaEntry-Objekt
    private MediaEntry mapRowToMedia(ResultSet rs) throws SQLException {
        MediaEntry media = new MediaEntry();
        media.setId(rs.getInt("id"));
        media.setTitle(rs.getString("title"));
        media.setDescription(rs.getString("description"));
        media.setMediaType(rs.getString("media_type"));
        media.setReleaseYear(rs.getInt("release_year"));

        // PostgreSQL TEXT[] Array zu Java List<String> konvertieren
        // Verschiedene JDBC-Treiber können unterschiedliche Array-Typen zurückgeben
        Array genresArray = rs.getArray("genres");
        if (genresArray != null) {
            try {
                Object arr = genresArray.getArray();
                if (arr instanceof String[] sarr) {
                    // Direktes String-Array
                    media.setGenres(Arrays.asList(sarr));
                } else if (arr instanceof Object[] oarr) {
                    // Object-Array (zu String konvertieren)
                    List<String> g = new ArrayList<>(oarr.length);
                    for (Object o : oarr) {
                        g.add(o != null ? o.toString() : null);
                    }
                    media.setGenres(g);
                } else {
                    media.setGenres(new ArrayList<>());
                }
            } finally {
                try {
                    genresArray.free();
                } catch (SQLException ignored) {
                }
            }
        } else {
            media.setGenres(new ArrayList<>());
        }

        media.setAgeRestriction(rs.getInt("age_restriction"));
        media.setCreatorId(rs.getInt("creator_id"));
        media.setAverageRating(rs.getDouble("average_score"));

        Timestamp timestamp = rs.getTimestamp("created_at");
        if (timestamp != null) {
            media.setCreatedAt(timestamp.toLocalDateTime());
        }
        return media;
    }
}
