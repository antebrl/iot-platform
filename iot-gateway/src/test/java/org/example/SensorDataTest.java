package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SensorDataTest {

    @Test
    void testGenerateRandomSensorIdInRange() {
        for (int i = 0; i < 100; i++) {
            SensorData data = SensorData.generateRandom();
            int sensorId = data.getSensorId();
            assertTrue(sensorId >= 1000 && sensorId < 2000, "SensorId sollte zwischen 1000 und 1999 liegen");
        }
    }

    @Test
    void testGenerateRandomTemperatureInRangeAndPrecision() {
        for (int i = 0; i < 100; i++) {
            SensorData data = SensorData.generateRandom();
            double temp = data.getTemperature();
            assertTrue(temp >= 15.0 && temp <= 30.0, "Temperatur sollte zwischen 15.0 und 30.0 liegen");

            // Prüfe, ob auf 2 Nachkommastellen gerundet
            String[] parts = String.valueOf(temp).split("\\.");
            if (parts.length == 2) {
                assertTrue(parts[1].length() <= 2, "Temperatur sollte höchstens 2 Nachkommastellen haben");
            }
        }
    }

    @Test
    void testToStringFormat() {
        SensorData data = SensorData.generateRandom();
        String result = data.toString();
        assertTrue(result.contains("Sensor ID: "), "String sollte 'Sensor ID: ' enthalten");
        assertTrue(result.contains("Temperatur: "), "String sollte 'Temperatur: ' enthalten");
    }

    @Test
    void testRoundMethod() {
        assertEquals(3.14, SensorData.round(3.14159, 2));
        assertEquals(3.0, SensorData.round(3.001, 0));
        assertThrows(IllegalArgumentException.class, () -> SensorData.round(3.14, -1));
    }
}