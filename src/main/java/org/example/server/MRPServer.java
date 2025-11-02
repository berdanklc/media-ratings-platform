package org.example.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.example.controller.AuthController;
import org.example.controller.MediaController;
import org.example.model.User;
import org.example.repository.MediaRepository;
import org.example.repository.RatingRepository;
import org.example.repository.UserRepository;
import org.example.service.AuthService;
import org.example.service.MediaService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Haupt-Server: Startet HTTP-Server, registriert alle Endpoints und handled das Routing
public class MRPServer {
    private final HttpServer server;
    private final AuthController authController;
    private final MediaController mediaController;
    private final AuthService authService;

    // Regex zum Parsen von URLs wie "/api/media/123" -> extrahiert die "123"
    private static final Pattern MEDIA_ID_PATTERN = Pattern.compile("/api/media/(\\d+)");

    public MRPServer(int port) throws IOException, SQLException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);

        // Dependency Injection: Repositories -> Services -> Controllers
        // Jede Schicht kennt nur die darunterliegende Schicht
        UserRepository userRepository = new UserRepository();
        MediaRepository mediaRepository = new MediaRepository();
        RatingRepository ratingRepository = new RatingRepository();

        this.authService = new AuthService(userRepository);
        MediaService mediaService = new MediaService(mediaRepository, ratingRepository);

        this.authController = new AuthController(authService);
        this.mediaController = new MediaController(mediaService);

        setupRoutes();
    }

    // Registriert alle HTTP-Endpoints beim Server
    // Context = ein URL-Pfad-Prefix
    private void setupRoutes() {
        server.createContext("/", this::handleRoot);
        server.createContext("/api/users/", this::handleAuthRoutes);
        server.createContext("/api/media", this::handleMediaRoutes);
        server.createContext("/api/media/", this::handleMediaRoutes);
    }

    // Root-Endpoint "/" für Health-Check (ob Server läuft)
    private void handleRoot(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("/".equals(path)) {
            String response = "{\"status\":\"MRP Server is running\",\"version\":\"1.0\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        } else {
            sendError(exchange, 404, "Not found");
        }
    }

    // Routing für Auth-Endpoints: /api/users/register und /api/users/login
    // Keine Authentifizierung nötig (das sind ja die Login-Endpoints)
    private void handleAuthRoutes(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        try {
            if ("/api/users/register".equals(path)) {
                authController.handleRegister(exchange);
            } else if ("/api/users/login".equals(path)) {
                authController.handleLogin(exchange);
            } else {
                sendError(exchange, 404, "Endpoint not found");
            }
        } catch (Exception e) {
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    // Routing für Media-Endpoints
    // Unterscheidet zwischen /api/media (alle) und /api/media/{id} (einzelner)
    private void handleMediaRoutes(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        Matcher mediaIdMatcher = MEDIA_ID_PATTERN.matcher(path);

        try {
            // Token validieren - User muss eingeloggt sein
            User authenticatedUser = authenticateRequest(exchange);

            // URL mit ID? (z.B. /api/media/5)
            if (mediaIdMatcher.matches()) {
                // Regex-Gruppe 1 ist die ID
                Integer mediaId = Integer.parseInt(mediaIdMatcher.group(1));
                switch (method) {
                    case "GET":
                        mediaController.handleGetMedia(exchange, mediaId);
                        break;
                    case "PUT":
                        // Update - Service prüft ob User = Creator
                        mediaController.handleUpdateMedia(exchange, mediaId, authenticatedUser);
                        break;
                    case "DELETE":
                        // Delete - Service prüft ob User = Creator
                        mediaController.handleDeleteMedia(exchange, mediaId, authenticatedUser);
                        break;
                    default:
                        sendError(exchange, 405, "Method Not Allowed");
                }
            } else if ("/api/media".equals(path)) {
                // Ohne ID = Listenoperationen
                switch (method) {
                    case "GET":
                        // Alle Media-Einträge holen
                        mediaController.handleGetAllMedia(exchange);
                        break;
                    case "POST":
                        // Neuen Media-Eintrag erstellen
                        mediaController.handleCreateMedia(exchange, authenticatedUser);
                        break;
                    default:
                        sendError(exchange, 405, "Method Not Allowed");
                }
            } else {
                sendError(exchange, 404, "Endpoint not found");
            }
        } catch (SecurityException e) {
            // Auth-Fehler
            if (e.getCause() instanceof SQLException) {
                sendError(exchange, 500, "Authentication failed due to server error.");
            } else {
                sendError(exchange, 401, "Unauthorized: " + e.getMessage());
            }
        } catch (Exception e) {
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    // Token-basierte Authentifizierung
    // Erwartet Header: "Authorization: Bearer <token>"
    // Gibt den eingeloggten User zurück oder wirft SecurityException
    private User authenticateRequest(HttpExchange exchange) throws SecurityException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Authorization header is missing or invalid.");
        }
        // "Bearer " abschneiden, Token extrahieren
        String token = authHeader.substring(7);
        try {
            Optional<User> userOpt = authService.validateToken(token);
            return userOpt.orElseThrow(() -> new SecurityException("Invalid or expired token."));
        } catch (SQLException e) {
            throw new SecurityException("Authentication failed due to database error.", e);
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        String responseBody = String.format("{\"error\":\"%s\"}", message.replace("\"", "\\\""));
        exchange.sendResponseHeaders(statusCode, responseBody.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBody.getBytes(StandardCharsets.UTF_8));
        }
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }
}
