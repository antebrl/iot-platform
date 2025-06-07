package org.example;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class IndependentSensorSimulator {

    private static final String BROKER_HOST = "hivemq"; // Hostname aus Docker Compose
    private static final int BROKER_PORT = 1883;

    private static final int INTERVAL = 3000; // Zeit zwischen Publishs
    private static final int NEW_SENSOR_INTERVAL = 5000; // Zeit zwischen Starts neuer Sensoren
    private static final int MAX_SENSORS = 20; // Maximale Anzahl gleichzeitig aktiver Sensoren

    private static final AtomicInteger sensorCounter = new AtomicInteger(1);
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Semaphore semaphore = new Semaphore(MAX_SENSORS);

    public static void main(String[] args) {
        System.out.println("Starte Sensor-Simulator...");

        while (true) {
            try {
                semaphore.acquire(); // Blockiert, wenn MAX_SENSORS erreicht
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

    static class TemperatureSensor implements Runnable {
        private final String sensorId;
        private final Random random = new Random();
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        public TemperatureSensor(String sensorId) {
            this.sensorId = sensorId;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(3000); // Kurzes Warten, z.B. nach Container-Start
            } catch (InterruptedException e) {
                System.err.println("[" + sensorId + "] Start unterbrochen.");
                semaphore.release();
                return;
            }

            Mqtt3AsyncClient client = MqttClient.builder()
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

                        // Start Publishing in separatem Thread
                        executor.submit(() -> {
                            try {
                                startPublishing(client);
                            } finally {
                                semaphore.release(); // Freigabe des Slots – auch bei Erfolg
                            }
                        });
                    });
        }

        private void startPublishing(Mqtt3AsyncClient client) {
            while (true) {
                try {
                    double temperature = 20 + random.nextDouble() * 10;
                    String timestamp = dateFormat.format(new Date());
                    String payload = String.format(
                            "{\"sensorId\":\"%s\",\"temperature\":%.2f,\"timestamp\":\"%s\"}",
                            sensorId, temperature, timestamp
                    );

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
