package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.net.httpserver.HttpExchange;
import org.example.model.User;
import org.example.service.AuthService;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

// Controller für Registrierung und Login
// Nimmt HTTP-Requests entgegen und delegiert an AuthService
public class AuthController {
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public AuthController(AuthService authService) {
        this.authService = authService;
        this.objectMapper = new ObjectMapper();
        // Jackson-Module für LocalDateTime Serialisierung
        // Ohne das würde LocalDateTime als Timestamp-Zahl statt als String serialisiert
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // POST /api/users/register
    // Erwartet JSON Body: {"username":"...", "password":"..."}
    // Gibt bei Erfolg 201 Created und User-Objekt zurück (Passwort wird gefiltert durch @JsonProperty)
    public void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendErrorResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            // JSON Body zu User-Objekt parsen
            User credentials = objectMapper.readValue(exchange.getRequestBody(), User.class);
            String username = credentials.getUsername();
            String password = credentials.getPassword();

            // Registrierung durchführen
            User registeredUser = authService.register(username, password);

            // 201 Created mit User-Daten (Passwort wird durch @JsonProperty(WRITE_ONLY) nicht zurückgegeben)
            sendJsonResponse(exchange, 201, registeredUser);

        } catch (IllegalArgumentException e) {
            // Validierungsfehler (z.B. Username existiert schon, Passwort zu kurz)
            sendErrorResponse(exchange, 400, e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    // POST /api/users/login
    // Erwartet JSON Body: {"username":"...", "password":"..."}
    // Gibt bei Erfolg 200 OK und Token zurück: {"token":"username-mrpToken-UUID"}
    public void handleLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendErrorResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            User credentials = objectMapper.readValue(exchange.getRequestBody(), User.class);
            String username = credentials.getUsername();
            String password = credentials.getPassword();

            // Login durchführen und Token erhalten
            String token = authService.login(username, password);

            // Token als JSON zurückgeben
            sendJsonResponse(exchange, 200, Map.of("token", token));

        } catch (IllegalArgumentException e) {
            // Falsche Credentials = 401 Unauthorized
            sendErrorResponse(exchange, 401, e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    // Hilfsmethode: Java-Objekt zu JSON serialisieren und als Response senden
    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        byte[] response = objectMapper.writeValueAsBytes(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    // Hilfsmethode: Fehler als JSON senden: {"error":"message"}
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, String> errorResponse = Map.of("error", message);
        sendJsonResponse(exchange, statusCode, errorResponse);
    }
}
