package org.example.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelClassesTest {

    // Keep only creation sanity tests to validate core model shapes
    @Test
    void testUserCreation() {
        User user = new User("testuser", "password123");
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
        assertEquals("password123", user.getPassword());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    void testMediaEntryCreation() {
        MediaEntry media = new MediaEntry("Inception", "A mind-bending thriller", "movie", 2010);
        assertNotNull(media);
        assertEquals("Inception", media.getTitle());
        assertEquals("A mind-bending thriller", media.getDescription());
        assertEquals("movie", media.getMediaType());
        assertEquals(2010, media.getReleaseYear());
        assertNotNull(media.getGenres());
        assertEquals(0.0, media.getAverageScore());
    }

    @Test
    void testRatingCreation() {
        Rating rating = new Rating(1, 2, 5, "Excellent!");
        assertNotNull(rating);
        assertEquals(1, rating.getUserId());
        assertEquals(2, rating.getMediaId());
        assertEquals(5, rating.getStars());
        assertEquals("Excellent!", rating.getComment());
        assertNotNull(rating.getTimestamp());
        assertEquals(0, rating.getLikes());
        assertFalse(rating.getConfirmed());
    }
}
