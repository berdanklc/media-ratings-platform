package org.example.service;

import org.example.exception.ForbiddenException;
import org.example.model.MediaEntry;
import org.example.repository.MediaRepository;
import org.example.repository.RatingRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// Service-Layer für Media-Verwaltung
// Enthält Business-Logik und Autorisierungsprüfungen
public class MediaService {
    private final MediaRepository mediaRepository;
    private final RatingRepository ratingRepository;

    public MediaService(MediaRepository mediaRepository, RatingRepository ratingRepository) {
        this.mediaRepository = mediaRepository;
        this.ratingRepository = ratingRepository;
    }

    // Neuen Media-Eintrag erstellen
    // Creator-ID wird automatisch gesetzt (ist der eingeloggte User)
    public MediaEntry createMedia(MediaEntry media, Integer creatorId) throws SQLException {
        media.setCreatorId(creatorId);
        return mediaRepository.save(media);
    }

    public Optional<MediaEntry> getMediaById(Integer id) throws SQLException {
        return mediaRepository.findById(id);
    }

    public List<MediaEntry> getAllMedia() throws SQLException {
        return mediaRepository.findAll();
    }

    // Media-Eintrag aktualisieren
    // Nur der Creator darf seinen eigenen Eintrag bearbeiten
    // requesterId = ID des eingeloggten Users
    public void updateMedia(Integer mediaId, MediaEntry updatedMedia, Integer requesterId) throws SQLException {
        // Prüfen ob Media existiert
        MediaEntry existingMedia = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Media with ID " + mediaId + " not found."));

        // Autorisierungsprüfung: Ist der eingeloggte User auch der Creator?
        if (!Objects.equals(existingMedia.getCreatorId(), requesterId)) {
            throw new SecurityException("User is not authorized to update this media entry.");
        }

        // ID muss gleich bleiben
        updatedMedia.setId(mediaId);
        mediaRepository.update(updatedMedia);
    }

    // Media-Eintrag löschen
    // Nur der Creator darf seinen eigenen Eintrag löschen
    public void deleteMedia(Integer mediaId, Integer requesterId) throws SQLException {
        // Prüfen ob Media existiert
        MediaEntry existingMedia = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Media with ID " + mediaId + " not found."));

        // Autorisierungsprüfung
        if (!Objects.equals(existingMedia.getCreatorId(), requesterId)) {
            throw new ForbiddenException("User is not authorized to delete this media entry.");
        }

        // Löscht auch automatisch alle zugehörigen Ratings (CASCADE)
        mediaRepository.delete(mediaId);
    }
}
