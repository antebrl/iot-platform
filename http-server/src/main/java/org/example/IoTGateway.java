package org.example;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class IoTGateway {
    public static void main(String[] args) throws InterruptedException {
        String host = "http-server";
        int port = 8080;

        while (true) {
            // Zufällige Sensordaten generieren
            SensorData data = SensorData.generateRandom();
            String json = data.toJson();

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

                System.out.println("Gesendet:\n" + json);

                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Antwort: " + line);
                }

            } catch (IOException e) {
                System.err.println("Verbindungsfehler: " + e.getMessage());
            }

            Thread.sleep(1000); // 1 Sekunde Pause
        }
    }
}
