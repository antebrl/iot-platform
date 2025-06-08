package org.example;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class IndependentSensorSimulator {

    static final String BROKER_HOST = "hivemq";
    static final int BROKER_PORT = 1883;

    static final int INTERVAL = 3000;
    static final int NEW_SENSOR_INTERVAL = 5000;
    static final int MAX_SENSORS = 20;

    static final AtomicInteger sensorCounter = new AtomicInteger(1);
    static final ExecutorService executor = Executors.newCachedThreadPool();
    static final Semaphore semaphore = new Semaphore(MAX_SENSORS);

    public static void main(String[] args) {
        System.out.println("Starte Sensor-Simulator...");

        while (true) {
            try {
                semaphore.acquire();
                String sensorId = "temp-sensor-" + sensorCounter.getAndIncrement();
                executor.submit(new TemperatureSensor(sensorId));
                Thread.sleep(NEW_SENSOR_INTERVAL);
            } catch (InterruptedException e) {
                System.err.println("Simulator unterbrochen.");
                break;
            }
        }

        executor.shutdown();
    }

    public static class TemperatureSensor implements Runnable {
        private final String sensorId;
        private final Random random = new Random();
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        private final Mqtt3AsyncClient clientOverride;

        // Konstruktor für Produktion (nur sensorId)
        public TemperatureSensor(String sensorId) {
            this(sensorId, null);
        }

        // Konstruktor für Tests (sensorId + optionaler Mock-Client)
        public TemperatureSensor(String sensorId, Mqtt3AsyncClient clientOverride) {
            this.sensorId = sensorId;
            this.clientOverride = clientOverride;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                System.err.println("[" + sensorId + "] Start unterbrochen.");
                semaphore.release();
                return;
            }

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
                            semaphore.release();
                            return;
                        }

                        System.out.printf("[%s] Erfolgreich verbunden mit HiveMQ.%n", sensorId);

                        executor.submit(() -> {
                            try {
                                startPublishing(client);
                            } finally {
                                semaphore.release();
                            }
                        });
                    });
        }

        private void startPublishing(Mqtt3AsyncClient client) {
            while (true) {
                try {
                    double temperature = 20 + random.nextDouble() * 10;
                    String timestamp = dateFormat.format(new Date());
                    String payload = generatePayload(temperature, timestamp);

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

        // Neu: Payload-Generierung extrahiert für Tests
        public String generatePayload(double temperature, String timestamp) {
            return String.format(Locale.US,
                    "{\"sensorId\":\"%s\",\"temperature\":%.2f,\"timestamp\":\"%s\"}",
                    sensorId, temperature, timestamp);
        }
    }
}
