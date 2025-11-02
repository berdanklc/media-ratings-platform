package org.example.service;

import org.example.exception.ForbiddenException;
import org.example.model.MediaEntry;
import org.example.repository.MediaRepository;
import org.example.repository.RatingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the MediaService.
 * These tests focus on the business logic for managing media entries, including
 * creation, retrieval, updates, and deletion, with a focus on authorization rules.
 */
class MediaServiceTest {

    @Mock
    private MediaRepository mediaRepository; // Mocked repository to isolate service logic.

    @Mock
    private RatingRepository ratingRepository; // Mocked, but not used for intermediate tests.

    private MediaService mediaService;

    // AutoCloseable returned by MockitoAnnotations.openMocks(this) so we can close it in @AfterEach
    private AutoCloseable mocks;

    private final int userCreatorId = 1;
    private final int otherUserId = 2;

    @BeforeEach
    void setUp() {
        // Initialize mocks and the service before each test.
        mocks = MockitoAnnotations.openMocks(this);
        mediaService = new MediaService(mediaRepository, ratingRepository);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    /**
     * Tests successful creation of a media entry.
     */
    @Test
    void testCreateMedia_Success() throws SQLException {
        // Arrange: A new media entry to be created.
        MediaEntry mediaToCreate = new MediaEntry(0, "Inception", "A mind-bending thriller.", "movie", 2010, List.of("Sci-Fi", "Thriller"), 12, 0);

        // Mock repository behavior: Simulate saving and returning the entry with a new ID.
        when(mediaRepository.save(any(MediaEntry.class))).thenAnswer(invocation -> {
            MediaEntry savedMedia = invocation.getArgument(0);
            savedMedia.setId(1); // Assign a database ID.
            return savedMedia;
        });

        // Act: Call the service method to create the media.
        MediaEntry result = mediaService.createMedia(mediaToCreate, userCreatorId);

        // Assert: The returned media entry has the correct ID and creator ID.
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(userCreatorId, result.getCreatorId());
        verify(mediaRepository, times(1)).save(mediaToCreate);
    }

    /**
     * Tests successful retrieval of a media entry by its ID.
     */
    @Test
    void testGetMediaById_Success() throws SQLException {
        // Arrange: An existing media entry.
        MediaEntry existingMedia = new MediaEntry(1, "The Matrix", "A classic sci-fi action film.", "movie", 1999, List.of("Sci-Fi", "Action"), 16, userCreatorId);
        when(mediaRepository.findById(1)).thenReturn(Optional.of(existingMedia));

        // Act: Attempt to retrieve the media by its ID.
        Optional<MediaEntry> result = mediaService.getMediaById(1);

        // Assert: The correct media entry is found and returned.
        assertTrue(result.isPresent());
        assertEquals("The Matrix", result.get().getTitle());
    }

    /**
     * Tests retrieval of a non-existent media entry.
     */
    @Test
    void testGetMediaById_NotFound() throws SQLException {
        // Arrange: Mock repository to return an empty Optional.
        when(mediaRepository.findById(anyInt())).thenReturn(Optional.empty());

        // Act: Attempt to retrieve a media entry that doesn't exist.
        Optional<MediaEntry> result = mediaService.getMediaById(99);

        // Assert: The result is an empty Optional.
        assertFalse(result.isPresent());
    }

    /**
     * Tests a successful media update by its creator.
     */
    @Test
    void testUpdateMedia_Success() throws SQLException {
        // Arrange: An existing media entry and the data to update it with.
        MediaEntry existingMedia = new MediaEntry(1, "Old Title", "Old Desc", "movie", 2020, List.of("Genre"), 12, userCreatorId);
        MediaEntry updatedInfo = new MediaEntry(0, "New Title", "New Desc", "movie", 2021, List.of("New Genre"), 16, 0);

        when(mediaRepository.findById(1)).thenReturn(Optional.of(existingMedia));
        doNothing().when(mediaRepository).update(any(MediaEntry.class));

        // Act: The creator updates the media.
        mediaService.updateMedia(1, updatedInfo, userCreatorId);

        // Assert: The repository's update method is called with the correct data.
        verify(mediaRepository, times(1)).update(updatedInfo);
        assertEquals(1, updatedInfo.getId()); // Ensure the ID was set correctly before updating.
    }

    /**
     * Tests that updating a media entry fails if the user is not the creator.
     */
    @Test
    void testUpdateMedia_Forbidden_NotCreator() throws SQLException {
        // Arrange: An existing media entry.
        MediaEntry existingMedia = new MediaEntry(1, "Title", "Desc", "movie", 2020, List.of("Genre"), 12, userCreatorId);
        MediaEntry updatedInfo = new MediaEntry(0, "New Title", null, null, 0, null, 0, 0);
        when(mediaRepository.findById(1)).thenReturn(Optional.of(existingMedia));

        // Act & Assert: An attempt by another user to update should throw a SecurityException.
        assertThrows(SecurityException.class, () -> mediaService.updateMedia(1, updatedInfo, otherUserId));
        verify(mediaRepository, never()).update(any(MediaEntry.class));
    }

    /**
     * Tests that updating a non-existent media entry fails.
     */
    @Test
    void testUpdateMedia_NotFound() throws SQLException {
        // Arrange: Mock repository to find no media.
        when(mediaRepository.findById(anyInt())).thenReturn(Optional.empty());
        MediaEntry updatedInfo = new MediaEntry();

        // Act & Assert: Expect an IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> mediaService.updateMedia(99, updatedInfo, userCreatorId));
    }

    /**
     * Tests successful deletion of a media entry by its creator.
     */
    @Test
    void testDeleteMedia_Success() throws SQLException {
        // Arrange: An existing media entry.
        MediaEntry existingMedia = new MediaEntry(1, "Title", "Desc", "movie", 2020, List.of("Genre"), 12, userCreatorId);
        when(mediaRepository.findById(1)).thenReturn(Optional.of(existingMedia));
        doNothing().when(mediaRepository).delete(1);

        // Act: The creator deletes the media.
        mediaService.deleteMedia(1, userCreatorId);

        // Assert: The repository's delete method was called.
        verify(mediaRepository, times(1)).delete(1);
    }

    /**
     * Tests that deleting a media entry fails if the user is not the creator.
     */
    @Test
    void testDeleteMedia_Forbidden_NotCreator() throws SQLException {
        // Arrange: An existing media entry.
        MediaEntry existingMedia = new MediaEntry(1, "Title", "Desc", "movie", 2020, List.of("Genre"), 12, userCreatorId);
        when(mediaRepository.findById(1)).thenReturn(Optional.of(existingMedia));

        // Act & Assert: An attempt by another user to delete should throw a ForbiddenException.
        assertThrows(ForbiddenException.class, () -> mediaService.deleteMedia(1, otherUserId));
    }
}
