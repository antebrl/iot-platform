package org.example;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.*;

public class IndependentSensorSimulator {

    static final String BROKER_HOST = "hivemq";
    static final int BROKER_PORT = 1883;
    static final Gson gson = new Gson();

    static final int INTERVAL = 3000;
    static final int MAX_RETRIES = 50;
    static final int RETRY_DELAY = 5000; // 5 seconds

    static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        System.out.println("Starte Sensor-Simulator...");

        int sensorId = SensorData.getRandomSensorId();
        executor.submit(new TemperatureSensor(sensorId));

        // Keep the main thread alive
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.err.println("Simulator unterbrochen.");
        }

        executor.shutdown();
    }

    public static class TemperatureSensor implements Runnable {
        private final int sensorId;
        private final Mqtt3AsyncClient clientOverride;
        private int retryCount = 0;

        // Konstruktor für Produktion (nur sensorId)
        public TemperatureSensor(int sensorId) {
            this(sensorId, null);
        }

        // Konstruktor für Tests (sensorId + optionaler Mock-Client)
        public TemperatureSensor(int sensorId, Mqtt3AsyncClient clientOverride) {
            this.sensorId = sensorId;
            this.clientOverride = clientOverride;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                System.err.println("[" + sensorId + "] Start unterbrochen.");
                return;
            }

            connectWithRetry();
        }

        private void connectWithRetry() {
            if (retryCount >= MAX_RETRIES) {
                System.err.printf("[%s] Maximale Anzahl an Verbindungsversuchen erreicht.%n", sensorId);
                return;
            }

            try {
                Mqtt3AsyncClient client = (clientOverride != null) ? clientOverride : MqttClient.builder()
                        .useMqttVersion3()
                        .identifier(UUID.randomUUID().toString())
                        .serverHost(BROKER_HOST)
                        .serverPort(BROKER_PORT)
                        .buildAsync();

                client.connectWith()
                        .cleanSession(true)
                        .send()
                        .whenComplete((connAck, throwable) -> {
                            if (throwable != null) {
                                System.err.printf("[%s] Verbindungsversuch fehlgeschlagen: %s%n", sensorId, throwable.getMessage());
                                retryCount++;
                                if (retryCount < MAX_RETRIES) {
                                    System.out.printf("[%s] Verbindungsversuch %d fehlgeschlagen. Warte %d Sekunden...%n", 
                                        sensorId, retryCount, RETRY_DELAY/1000);
                                    try {
                                        Thread.sleep(RETRY_DELAY);
                                        connectWithRetry();
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                    }
                                } else {
                                    System.err.printf("[%s] Maximale Anzahl an Verbindungsversuchen erreicht.%n", sensorId);
                                }
                                return;
                            }

                            System.out.printf("[%s] Erfolgreich verbunden mit HiveMQ.%n", sensorId);
                            executor.submit(() -> startPublishing(client));
                        });
            } catch (Exception e) {
                System.err.printf("[%s] Fehler beim Verbindungsaufbau: %s%n", sensorId, e.getMessage());
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

        private void startPublishing(Mqtt3AsyncClient client) {
            while (true) {
                try {
                    SensorData data = SensorData.generateRandom(sensorId);
                    String payload = gson.toJson(data);

                    String topic = "sensor/temperature/" + sensorId;

                    client.publishWith()
                            .topic(topic)
                            .payload(payload.getBytes(StandardCharsets.UTF_8))
                            .send()
                            .thenAccept(unused ->
                                    System.out.printf("[%s] Gesendet → %s: %s%n", sensorId, topic, payload)
                            );

                    Thread.sleep(INTERVAL);
                } catch (Exception e) {
                    System.err.printf("[%s] Fehler beim Senden: %s%n", sensorId, e.getMessage());
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
}
