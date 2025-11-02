package org.example.exception;

/**
 * Exception für 403 Forbidden - wird geworfen wenn ein User keine Berechtigung hat.
 * Beispiel: User versucht einen MediaEntry zu löschen, den ein anderer User erstellt hat.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}

