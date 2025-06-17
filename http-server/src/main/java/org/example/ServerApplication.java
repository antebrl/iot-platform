package org.example;
import org.example.db.DataStorage;
import org.example.db.InMemoryDataStorage;
import org.example.db.HazelcastDataStorage;
import org.example.db.RedundantDataStorage;

import java.io.IOException;

/**
 * Main application class that starts the HTTP server.
 * This class is responsible for initializing and running the server.
 */
public class ServerApplication {
    
    /**
     * Main method to start the HTTP server
     * 
     * @param args Command line arguments (not used)
     * @throws IOException If there's an error starting the server
     */
    public static void main(String[] args) throws IOException {
        // Speicherungsredundanz
        DataStorage inMemoryStorage = new InMemoryDataStorage();
        DataStorage hazelcastStorage = new HazelcastDataStorage(); // musst du erstellen
        DataStorage redundantStorage = new RedundantDataStorage(inMemoryStorage, hazelcastStorage);

        HttpServer server = new HttpServer(8080, redundantStorage);
        System.out.println("Starting HTTP server...");
        server.start();
        
        // Add shutdown hook to gracefully stop the server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
            System.out.println("Server stopped.");
        }));
    }
}
