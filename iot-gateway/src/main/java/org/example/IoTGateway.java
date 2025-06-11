package org.example;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class IoTGateway {
    private static final Gson gson = new Gson();
    private static final String MQTT_BROKER = "hivemq";
    private static final int MQTT_PORT = 1883;

    private static final String HTTP_SERVER = System.getenv().getOrDefault("HTTP_SERVER_HOST", "localhost");
    private static final int HTTP_PORT = 8080;

    private static final int MAX_RETRIES = 5;
    private static final int RETRY_DELAY = 5000; // 5 seconds
    private static int retryCount = 0;

    public static void main(String[] args) {
        System.out.println("Starte IoT-Gateway...");
        
        while (true) {
            try {
                connectWithRetry();
                // Wenn wir hier ankommen, war die Verbindung erfolgreich
                // Warte auf Unterbrechung
                Thread.currentThread().join();
            } catch (Exception e) {
                System.err.println("Unerwarteter Fehler: " + e.getMessage());
                Thread.sleep(RETRY_DELAY);
            }
        }
    }

    private static void connectWithRetry() {
        if (retryCount >= MAX_RETRIES) {
            System.err.println("Maximale Anzahl an Verbindungsversuchen erreicht.");
            return;
        }

        try {
            Mqtt3AsyncClient mqttClient = MqttClient.builder()
                    .useMqttVersion3()
                    .identifier("iot-gateway-" + UUID.randomUUID())
                    .serverHost(MQTT_BROKER)
                    .serverPort(MQTT_PORT)
                    .buildAsync();

            mqttClient.connectWith()
                    .cleanSession(true)
                    .send()
                    .whenComplete((connAck, throwable) -> {
                        if (throwable != null) {
                            System.err.println("MQTT Verbindungsfehler: " + throwable.getMessage());
                            retryCount++;
                            if (retryCount < MAX_RETRIES) {
                                System.out.printf("Verbindungsversuch %d fehlgeschlagen. Warte %d Sekunden...%n", 
                                    retryCount, RETRY_DELAY/1000);
                                try {
                                    Thread.sleep(RETRY_DELAY);
                                    connectWithRetry();
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                }
                            } else {
                                System.err.println("Maximale Anzahl an Verbindungsversuchen erreicht.");
                            }
                            return;
                        }

                        // Erfolgreiche Verbindung - Counter zurücksetzen
                        retryCount = 0;
                        System.out.println("Erfolgreich mit HiveMQ verbunden");

                        // Auf alle Temperatursensoren subscriben
                        mqttClient.subscribeWith()
                                .topicFilter("sensor/temperature/#")
                                .callback(publish -> {
                                    String topic = publish.getTopic().toString();
                                    String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                                    System.out.println("Empfangen von " + topic + ": " + payload);

                                    try {
                                        // SensorData aus JSON parsen
                                        SensorData data = gson.fromJson(payload, SensorData.class);
                                        
                                        // HTTP Request vorbereiten
                                        String json = gson.toJson(data);
                                        String httpRequest = 
                                            "POST / HTTP/1.1\r\n" +
                                            "Host: " + HTTP_SERVER + "\r\n" +
                                            "Content-Type: application/json\r\n" +
                                            "Content-Length: " + json.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                                            "Connection: close\r\n" +
                                            "\r\n" +
                                            json;

                                        // HTTP Request senden
                                        try (Socket socket = new Socket(HTTP_SERVER, HTTP_PORT);
                                             OutputStream output = socket.getOutputStream();
                                             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                                            output.write(httpRequest.getBytes(StandardCharsets.UTF_8));
                                            output.flush();

                                            // Antwort lesen
                                            String line;
                                            System.out.println("\nHTTP Response:");
                                            while ((line = reader.readLine()) != null) {
                                                System.out.println(line);
                                            }
                                        }
                                    } catch (Exception e) {
                                        System.err.println("Fehler beim Verarbeiten der Nachricht: " + e.getMessage());
                                    }
                                })
                                .send()
                                .whenComplete((subAck, subscribeError) -> {
                                    if (subscribeError != null) {
                                        System.err.println("Fehler beim Subscriben: " + subscribeError.getMessage());
                                        return;
                                    }
                                    System.out.println("Erfolgreich auf Temperatursensoren subscribed");
                                });
                    });
        } catch (Exception e) {
            System.err.println("Fehler beim Verbindungsaufbau: " + e.getMessage());
            retryCount++;
            if (retryCount < MAX_RETRIES) {
                try {
                    Thread.sleep(RETRY_DELAY);
                    connectWithRetry();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
