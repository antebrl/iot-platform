package org.example;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import com.google.gson.Gson;

public class IoTGateway {
    private static final Gson gson = new Gson();
    private static final Random random = new Random();

    // 3-7s sleep time
    private static final int SLEEP_TIME_VARIATION = 4000; // in ms

    public static void main(String[] args) throws InterruptedException {
        String host = System.getenv().getOrDefault("HTTP_SERVER_HOST", "localhost");
        int port = 8080;

        while (true) {
            // Zufällige Sensordaten generieren
            SensorData data = SensorData.generateRandom();
            String json = gson.toJson(data);

            // HTTP-POST-Anfrage aufbauen
            String httpRequest =
                    "POST /data HTTP/1.1\r\n" +
                            "Host: " + host + "\r\n" +
                            "Content-Type: application/json\r\n" +
                            "Content-Length: " + json.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n" +
                            json;

            // Senden und Antwort empfangen
            try (Socket socket = new Socket(host, port);
                 OutputStream output = socket.getOutputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                output.write(httpRequest.getBytes(StandardCharsets.UTF_8));
                output.flush();

                String line;
                System.out.println("\nResponse: ");
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }

            } catch (IOException e) {
                System.err.println("Connection failure: " + e.getMessage());
            }

            int sleepTime = random.nextInt(SLEEP_TIME_VARIATION);
            Thread.sleep(sleepTime); // 5 second
        }
    }
}
