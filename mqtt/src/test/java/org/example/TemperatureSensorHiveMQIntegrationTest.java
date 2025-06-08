package org.example;

import static org.junit.jupiter.api.Assertions.*;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import org.example.IndependentSensorSimulator.TemperatureSensor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Testcontainers
@ExtendWith(org.testcontainers.junit.jupiter.TestcontainersExtension.class)
public class TemperatureSensorHiveMQIntegrationTest {

    @Container
    private static final GenericContainer<?> hivemq = new GenericContainer<>("hivemq/hivemq-ce")
            .withExposedPorts(1883);

    @Test
    void testSensorPublishesAndReceiverReceives() throws Exception {
        String sensorId = "test-sensor";
        String topic = "sensor/temperature/" + sensorId;
        CountDownLatch latch = new CountDownLatch(1);

        String brokerHost = hivemq.getHost();
        Integer brokerPort = hivemq.getMappedPort(1883);

        Mqtt3AsyncClient receiver = MqttClient.builder()
                .useMqttVersion3()
                .identifier("receiver-client")
                .serverHost(brokerHost)
                .serverPort(brokerPort)
                .buildAsync();

        receiver.connectWith().cleanSession(true).send().get();

        receiver.subscribeWith()
                .topicFilter(topic)
                .callback(publish -> {
                    String payload = new String(publish.getPayloadAsBytes());
                    System.out.println("Empfangen: " + payload);
                    latch.countDown();
                })
                .send().get();

        Mqtt3AsyncClient sensorClient = MqttClient.builder()
                .useMqttVersion3()
                .identifier(UUID.randomUUID().toString())
                .serverHost(brokerHost)
                .serverPort(brokerPort)
                .buildAsync();

        new Thread(new TemperatureSensor(sensorId, sensorClient)).start();

        boolean received = latch.await(10, TimeUnit.SECONDS);
        assertTrue(received, "Die Nachricht wurde nicht empfangen.");
    }

    @Test
    void testGeneratePayloadFormat() {
        String sensorId = "sensor-test";
        double temperature = 23.45;
        String timestamp = "2025-06-08T12:34:56.789+02:00";

        TemperatureSensor sensor = new TemperatureSensor(sensorId);
        String payload = sensor.generatePayload(temperature, timestamp);

        System.out.println("Payload: " + payload);

        assertTrue(payload.contains(sensorId));
        // Flexiblere Prüfung: Temperaturwert als String, da z.B. 23.45 oder 23.4500000 vorkommen kann
        assertTrue(payload.matches(".*\"temperature\"\\s*:\\s*23\\.45.*"), "Payload Temperatur stimmt nicht im Format");
        assertTrue(payload.contains("\"timestamp\":\"2025-06-08T12:34:56.789+02:00\""));
        assertTrue(payload.startsWith("{") && payload.endsWith("}"));
    }

    @Test
    void testHandlesConnectionFailureGracefully() throws InterruptedException {
        // MQTT-Client mit ungültigem Host bauen
        Mqtt3AsyncClient faultyClient = MqttClient.builder()
                .useMqttVersion3()
                .identifier("faulty-client")
                .serverHost("unreachable-host")
                .serverPort(1883)
                .buildAsync();

        TemperatureSensor sensor = new TemperatureSensor("broken-sensor", faultyClient);

        Thread thread = new Thread(sensor);
        thread.start();

        thread.join(7000);

        assertFalse(thread.isAlive(), "Thread sollte bei Verbindungsfehler beendet sein");
    }

    @Test
    void testSemaphoreReleasedAfterSensorFinish() throws InterruptedException {
        int availableBefore = IndependentSensorSimulator.semaphore.availablePermits();

        TemperatureSensor sensor = new TemperatureSensor("test-sensor");
        Thread thread = new Thread(sensor);
        thread.start();

        Thread.sleep(7000);

        int availableAfter = IndependentSensorSimulator.semaphore.availablePermits();

        assertTrue(availableAfter >= availableBefore, "Semaphore sollte wieder freigegeben sein");
    }
}
