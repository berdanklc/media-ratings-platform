package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import org.example.exception.ForbiddenException;
import org.example.model.MediaEntry;
import org.example.model.User;
import org.example.service.MediaService;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

// Controller für alle Media CRUD-Operationen
public class MediaController {
    private final MediaService mediaService;
    private final ObjectMapper objectMapper;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    // POST /api/media - neuen Media-Eintrag erstellen
    public void handleCreateMedia(HttpExchange exchange, User authenticatedUser) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        try {
            MediaEntry media = objectMapper.readValue(exchange.getRequestBody(), MediaEntry.class);

            // Titel ist Pflichtfeld
            if (media.getTitle() == null || media.getTitle().trim().isEmpty()) {
                sendResponse(exchange, 400, "{\"error\":\"Title is required\"}");
                return;
            }

            // Creator-ID wird automatisch gesetzt
            MediaEntry createdMedia = mediaService.createMedia(media, authenticatedUser.getId());

            sendJsonResponse(exchange, 201, createdMedia);
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"Internal server error: " + e.getMessage() + "\"}");
        }
    }

    // GET /api/media/{id}
    public void handleGetMedia(HttpExchange exchange, Integer mediaId) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        try {
            Optional<MediaEntry> mediaOpt = mediaService.getMediaById(mediaId);

            if (mediaOpt.isPresent()) {
                sendJsonResponse(exchange, 200, mediaOpt.get());
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Media not found\"}");
            }
        } catch (SQLException e) {
            sendResponse(exchange, 500, "{\"error\":\"Database error while fetching media.\"}");
        }
    }

    // GET /api/media - alle Media-Einträge
    public void handleGetAllMedia(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        try {
            List<MediaEntry> results = mediaService.getAllMedia();
            sendJsonResponse(exchange, 200, results);
        } catch (SQLException e) {
            sendResponse(exchange, 500, "{\"error\":\"Database error while fetching all media.\"}");
        }
    }

    // PUT /api/media/{id} - nur Creator darf updaten
    public void handleUpdateMedia(HttpExchange exchange, Integer mediaId, User authenticatedUser) throws IOException {
        if (!"PUT".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        try {
            MediaEntry updatedMedia = objectMapper.readValue(exchange.getRequestBody(), MediaEntry.class);

            if (updatedMedia.getTitle() == null || updatedMedia.getTitle().trim().isEmpty()) {
                sendResponse(exchange, 400, "{\"error\":\"Title is required\"}");
                return;
            }

            // Service prüft ob User auch Creator ist
            mediaService.updateMedia(mediaId, updatedMedia, authenticatedUser.getId());

            sendResponse(exchange, 200, "{\"message\":\"Media updated successfully\"}");
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 404, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (SecurityException e) {
            sendResponse(exchange, 403, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"Internal server error: " + e.getMessage() + "\"}");
        }
    }

    // DELETE /api/media/{id} - nur Creator darf löschen
    public void handleDeleteMedia(HttpExchange exchange, Integer mediaId, User authenticatedUser) throws IOException {
        if (!"DELETE".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        try {
            mediaService.deleteMedia(mediaId, authenticatedUser.getId());
            // 204 No Content bei erfolgreicher Löschung
            exchange.sendResponseHeaders(204, -1);
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 404, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (ForbiddenException e) {
            sendResponse(exchange, 403, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"Internal server error: " + e.getMessage() + "\"}");
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        byte[] response = objectMapper.writeValueAsBytes(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        String jsonMessage = message;
        if (!message.trim().startsWith("{")) {
            jsonMessage = "{\"message\":\"" + message.replace("\"", "\\\"") + "\"}";
        }
        byte[] response = jsonMessage.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}
