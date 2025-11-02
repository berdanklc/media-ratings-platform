package org.example.service;

import org.example.model.User;
import org.example.repository.UserRepository;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

// Service-Layer für User-Authentifizierung
// Enthält die Business-Logik für Register/Login/Token-Validierung
public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Neuen User registrieren
    // Validiert Input und prüft ob Username schon existiert
    public User register(String username, String password) throws SQLException {
        // Input-Validierung
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (password == null || password.length() < 3) {
            throw new IllegalArgumentException("Password must be at least 3 characters long");
        }

        // Prüfen ob Username schon vergeben ist
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        // User in DB speichern
        User user = new User(username, password);
        return userRepository.save(user);
    }

    // User einloggen und Token generieren
    // Token wird in DB gespeichert für spätere Validierung
    public String login(String username, String password) throws SQLException {
        // User aus DB holen
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        // Passwort-Check (Plaintext für dieses Lernprojekt - Produktion würde BCrypt verwenden)
        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        // Token generieren im Format: "username-mrpToken-UUID"
        // Beispiel: "berdan-mrpToken-c2182eeb-418b-4fec-b637-ea235775b0cb"
        String token = username + "-mrpToken-" + UUID.randomUUID();
        userRepository.updateToken(user.getId(), token);

        return token;
    }

    // Token validieren und zugehörigen User zurückgeben
    // Wird bei jedem geschützten Request aufgerufen
    public Optional<User> validateToken(String token) throws SQLException, SecurityException {
        if (token == null || token.isEmpty()) {
            throw new SecurityException("Token cannot be empty");
        }
        // Token aus DB suchen
        return userRepository.findByToken(token);
    }
}
