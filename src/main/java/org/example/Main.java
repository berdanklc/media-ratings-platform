// java
package org.example;

import org.example.server.MRPServer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

// Hauptklasse: Startet den Server
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final int PORT = 8080;
    private static final int TIMEOUT_SECONDS = 60;

    public static void main(String[] args) {
        try {
            System.out.println("Starting Media Ratings Platform (MRP) Server...");
            System.out.println("=".repeat(50));

            // Server erstellen und starten
            MRPServer server = new MRPServer(PORT);
            server.start();

            // Warten bis Server bereit ist
            // Wichtig bei docker-compose: DB braucht oft länger zum Starten
            if (waitForServerReady(PORT, TIMEOUT_SECONDS)) {
                System.out.println("=".repeat(50));
                System.out.println("Server successfully started on port " + PORT);
                System.out.println("Press Ctrl+C to stop the server");
                // Hauptthread blockieren, damit Server läuft
                Thread.currentThread().join();
            } else {
                throw new IllegalStateException("Server did not become ready in time");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start server", e);
            System.exit(1);
        }
    }

    // Prüft ob der Server-Port erreichbar ist
    // Versucht alle 500ms eine Socket-Verbindung aufzubauen
    // Gibt true zurück wenn erfolgreich, false nach Timeout
    private static boolean waitForServerReady(int port, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try (Socket socket = new Socket()) {
                // Versuche Verbindung zum Server aufzubauen
                socket.connect(new InetSocketAddress("localhost", port), 1000);
                System.out.println("Server is ready on port " + port);
                return true;
            } catch (Exception e) {
                // Server noch nicht bereit, weiter versuchen
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        System.err.println("Server did not become ready within " + timeoutSeconds + " seconds");
        return false;
    }
}
