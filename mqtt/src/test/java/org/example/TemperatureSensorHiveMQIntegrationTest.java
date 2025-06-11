package org.example;

import static org.junit.jupiter.api.Assertions.*;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.google.gson.Gson;
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

    private static final Gson gson = new Gson();

    @Container
    private static final GenericContainer<?> hivemq = new GenericContainer<>("hivemq/hivemq-ce")
            .withExposedPorts(1883);

    @Test
    void testSensorPublishesAndReceiverReceives() throws Exception {
        int sensorId = 123; // Using a fixed test sensor ID
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
                    // Verify the payload contains the correct sensor ID and temperature
                    SensorData data = gson.fromJson(payload, SensorData.class);
                    assertEquals(sensorId, data.getSensorId());
                    assertTrue(data.getTemperature() >= 5 && data.getTemperature() <= 30);
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
    void testHandlesConnectionFailureGracefully() throws InterruptedException {
        // MQTT-Client mit ungültigem Host bauen
        Mqtt3AsyncClient faultyClient = MqttClient.builder()
                .useMqttVersion3()
                .identifier("faulty-client")
                .serverHost("unreachable-host")
                .serverPort(1883)
                .buildAsync();

        TemperatureSensor sensor = new TemperatureSensor(999, faultyClient);

        Thread thread = new Thread(sensor);
        thread.start();

        thread.join(7000);

        assertFalse(thread.isAlive(), "Thread sollte bei Verbindungsfehler beendet sein");
    }
}
