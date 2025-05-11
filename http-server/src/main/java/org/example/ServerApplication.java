package org.example;

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
        HttpServer server = new HttpServer();
        System.out.println("Starting HTTP server...");
        server.start();
        
        // Add shutdown hook to gracefully stop the server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
            System.out.println("Server stopped.");
        }));
    }
}
