package org.example;

public class ServerApplication {

    public static void main(String[] args) {
        try {
            // Using the default constructor which already sets up 2PC coordinator
            HttpServer server = new HttpServer();
            System.out.println("Starting HTTP server with 2PC coordination...");
            server.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                server.stop();
                System.out.println("Server stopped.");
            }));
        } catch (Exception e) {
            System.err.println("Fehler beim Starten des Servers: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
