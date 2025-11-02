package org.example.repository;

import org.example.database.DatabaseConnection;
import org.example.model.User;

import java.sql.*;
import java.util.Optional;

// Repository für User-Tabelle
// Alle Datenbankoperationen für User-Accounts
public class UserRepository {
    private final Connection connection;

    public UserRepository() throws SQLException {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    // User in DB speichern (INSERT)
    // RETURNING gibt automatisch generierte Werte (id, created_at) zurück
    public User save(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?) RETURNING id, created_at";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // Auto-generierte ID und Timestamp zurückholen
                user.setId(rs.getInt("id"));
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            return user;
        }
    }

    // User anhand Username suchen (für Login)
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToUser(rs));
            }
            return Optional.empty();
        }
    }

    // User anhand Token suchen (für Authentifizierung bei jedem Request)
    public Optional<User> findByToken(String token) throws SQLException {
        String sql = "SELECT * FROM users WHERE token = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, token);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToUser(rs));
            }
            return Optional.empty();
        }
    }

    public Optional<User> findById(Integer id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToUser(rs));
            }
            return Optional.empty();
        }
    }

    // Token in DB speichern (nach erfolgreichem Login)
    public void updateToken(Integer userId, String token) throws SQLException {
        String sql = "UPDATE users SET token = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, token);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    // User-Profil aktualisieren (Email und Favorite Genre)
    public void updateProfile(Integer userId, String email, String favoriteGenre) throws SQLException {
        String sql = "UPDATE users SET email = ?, favorite_genre = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, favoriteGenre);
            stmt.setInt(3, userId);
            stmt.executeUpdate();
        }
    }

    // Konvertiert Datenbank-Zeile zu User-Objekt
    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEmail(rs.getString("email"));
        user.setFavoriteGenre(rs.getString("favorite_genre"));
        user.setToken(rs.getString("token"));
        Timestamp timestamp = rs.getTimestamp("created_at");
        if (timestamp != null) {
            user.setCreatedAt(timestamp.toLocalDateTime());
        }
        return user;
    }
}
