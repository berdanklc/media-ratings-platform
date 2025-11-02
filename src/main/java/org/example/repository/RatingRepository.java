package org.example.repository;

import org.example.database.DatabaseConnection;
import org.example.model.Rating;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository für Rating-Datenzugriff.
 * SOLID-Prinzip: Single Responsibility - zuständig nur für Rating-Persistierung.
 */
public class RatingRepository {
    private final Connection connection;

    public RatingRepository() throws SQLException {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public Rating save(Rating rating) throws SQLException {
        String sql = "INSERT INTO ratings (user_id, media_id, stars, comment, confirmed) VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, rating.getUserId());
            stmt.setInt(2, rating.getMediaId());
            stmt.setInt(3, rating.getStars());
            stmt.setString(4, rating.getComment());
            stmt.setBoolean(5, rating.getConfirmed());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                rating.setId(rs.getInt("id"));
            }
            return rating;
        }
    }

    public Optional<Rating> findById(Integer id) throws SQLException {
        String sql = "SELECT * FROM ratings WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToRating(rs));
            }
            return Optional.empty();
        }
    }

    public Optional<Rating> findByUserAndMedia(Integer userId, Integer mediaId) throws SQLException {
        String sql = "SELECT * FROM ratings WHERE user_id = ? AND media_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, mediaId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToRating(rs));
            }
            return Optional.empty();
        }
    }

    public List<Rating> findByUserId(Integer userId) throws SQLException {
        List<Rating> ratings = new ArrayList<>();
        String sql = "SELECT * FROM ratings WHERE user_id = ? ORDER BY timestamp DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ratings.add(mapRowToRating(rs));
            }
        }
        return ratings;
    }

    public List<Rating> findByMediaId(Integer mediaId) throws SQLException {
        List<Rating> ratings = new ArrayList<>();
        String sql = "SELECT * FROM ratings WHERE media_id = ? ORDER BY timestamp DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, mediaId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ratings.add(mapRowToRating(rs));
            }
        }
        return ratings;
    }

    public void update(Rating rating) throws SQLException {
        String sql = "UPDATE ratings SET stars = ?, comment = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, rating.getStars());
            stmt.setString(2, rating.getComment());
            stmt.setInt(3, rating.getId());
            stmt.executeUpdate();
        }
    }

    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM ratings WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * Bestätigt einen Kommentar - nur der Media-Creator kann dies tun.
     */
    public void confirmComment(Integer ratingId) throws SQLException {
        String sql = "UPDATE ratings SET confirmed = TRUE WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, ratingId);
            stmt.executeUpdate();
        }
    }

    /**
     * Prüft ob ein User ein Rating bereits geliked hat.
     */
    public boolean hasUserLikedRating(Integer ratingId, Integer userId) throws SQLException {
        String sql = "SELECT 1 FROM rating_likes WHERE rating_id = ? AND user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, ratingId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    /**
     * Fügt ein Like zu einem Rating hinzu und aktualisiert den Like-Counter.
     */
    public void addLike(Integer ratingId, Integer userId) throws SQLException {
        // ON CONFLICT DO NOTHING: verhindert doppelte Likes
        String sql = "INSERT INTO rating_likes (rating_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, ratingId);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }

        // Aktualisiere den Like-Counter
        String updateSql = "UPDATE ratings SET likes = (SELECT COUNT(*) FROM rating_likes WHERE rating_id = ?) WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            stmt.setInt(1, ratingId);
            stmt.setInt(2, ratingId);
            stmt.executeUpdate();
        }
    }

    /**
     * Fügt einen MediaEntry zur Favoritenliste eines Users hinzu.
     */
    public void addFavorite(Integer userId, Integer mediaId) throws SQLException {
        String sql = "INSERT INTO favorites (user_id, media_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, mediaId);
            stmt.executeUpdate();
        }
    }

    public void removeFavorite(Integer userId, Integer mediaId) throws SQLException {
        String sql = "DELETE FROM favorites WHERE user_id = ? AND media_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, mediaId);
            stmt.executeUpdate();
        }
    }

    public List<Integer> getFavoriteMediaIds(Integer userId) throws SQLException {
        List<Integer> mediaIds = new ArrayList<>();
        String sql = "SELECT media_id FROM favorites WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                mediaIds.add(rs.getInt("media_id"));
            }
        }
        return mediaIds;
    }

    public List<Rating> getRatingsByUserId(int userId) throws SQLException {
        return findByUserId(userId);
    }

    // Helper: Wandelt eine Datenbankzeile in ein Rating-Objekt um
    private Rating mapRowToRating(ResultSet rs) throws SQLException {
        Rating rating = new Rating();
        rating.setId(rs.getInt("id"));
        rating.setUserId(rs.getInt("user_id"));
        rating.setMediaId(rs.getInt("media_id"));
        rating.setStars(rs.getInt("stars"));
        rating.setComment(rs.getString("comment"));
        rating.setLikes(rs.getInt("likes"));
        rating.setConfirmed(rs.getBoolean("confirmed"));

        Timestamp timestamp = rs.getTimestamp("timestamp");
        if (timestamp != null) {
            rating.setTimestamp(timestamp.toLocalDateTime());
        }
        return rating;
    }
}
