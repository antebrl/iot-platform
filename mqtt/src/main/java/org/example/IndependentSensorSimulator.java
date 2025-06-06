package org.example;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class IndependentSensorSimulator {

    private static final String CLIENT_ID = "sensor-simulator-01";

    private static volatile boolean running = true;

    public static void main(String[] args) throws InterruptedException {
        String mqttBrokerHost = System.getenv().getOrDefault("MQTT_BROKER", "localhost");
        int mqttBrokerPort = 1883;

        Mqtt3AsyncClient client = createClient(mqttBrokerHost, mqttBrokerPort);

        connectWithRetry(client);

        ExecutorService executor = Executors.newFixedThreadPool(4);

        executor.submit(() -> runSensor(client, "sensors/temperature", "temperature", "°C", 20, 30, 5));
        executor.submit(() -> runSensor(client, "sensors/humidity", "humidity", "%", 30, 80, 7));
        executor.submit(() -> runSensor(client, "sensors/light", "light", "Lux", 100, 1000, 10));
        executor.submit(() -> runSensor(client, "sensors/pressure", "pressure", "hPa", 980, 1020, 6));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown requested, stopping sensors...");
            running = false;
            executor.shutdownNow();
            try {
                if (client.getState() == MqttClientState.CONNECTED) {
                    client.disconnect().get();
                    System.out.println("MQTT client disconnected.");
                }
            } catch (Exception e) {
                System.err.println("Fehler beim Disconnect: " + e.getMessage());
            }
        }));

        // Keep main thread alive while sensors run
        while (running) {
            Thread.sleep(1000);
        }
    }

    private static Mqtt3AsyncClient createClient(String host, int port) {
        return MqttClient.builder()
                .useMqttVersion3()
                .identifier(CLIENT_ID)
                .serverHost(host)
                .serverPort(port)
                .buildAsync();
    }

    private static void connectWithRetry(Mqtt3AsyncClient client) {
        while (running) {
            try {
                client.connectWith().send().get();
                System.out.println("Verbunden mit MQTT-Broker.");
                break;
            } catch (Exception e) {
                System.err.println("Verbindung fehlgeschlagen: " + e.getMessage() + ", versuche erneut in 5 Sekunden...");
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private static void runSensor(Mqtt3AsyncClient client, String topic, String type, String unit, double min, double max, int intervalSeconds) {
        Random random = new Random();

        while (running && !Thread.currentThread().isInterrupted()) {
            double value = min + ((max - min) * random.nextDouble());
            String timestamp = Instant.now().toString();

            String payload = String.format(
                    "{ \"sensorId\": \"%s\", \"timestamp\": \"%s\", \"type\": \"%s\", \"value\": %.2f, \"unit\": \"%s\" }",
                    CLIENT_ID, timestamp, type, value, unit);

            client.publishWith()
                    .topic(topic)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .payload(payload.getBytes(StandardCharsets.UTF_8))
                    .send()
                    .whenComplete((pub, throwable) -> {
                        if (throwable != null) {
                            System.err.println("Fehler beim Senden an " + topic + ": " + throwable.getMessage());
                        } else {
                            System.out.println("[" + topic + "] Gesendet: " + payload);
                        }
                    });

            try {
                TimeUnit.SECONDS.sleep(intervalSeconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("Sensor " + type + " beendet.");
    }
}
