package org.example;

import org.example.db.DataStorage;
import org.example.db.HazelcastDataStorage;
import org.example.db.InMemoryDataStorage;
import org.example.db.RedundantDataStorage;

public class ServerApplication {

    public static void main(String[] args) {
        try {
            DataStorage inMemoryStorage = new InMemoryDataStorage();
            HazelcastDataStorage hazelcastStorage = new HazelcastDataStorage();

            DataStorage redundantStorage = new RedundantDataStorage(inMemoryStorage, hazelcastStorage.getHazelcastInstance());

            HttpServer server = new HttpServer(8080, redundantStorage, hazelcastStorage);
            System.out.println("Starting HTTP server...");
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
