package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SensorDataTest {

    @Test
    void testGenerateRandomSensorIdInRange() {
        for (int i = 0; i < 100; i++) {
            SensorData data = SensorData.generateRandom();            
            int sensorId = data.getSensorId();
            assertTrue(sensorId >= 1000 && sensorId < 2000, "SensorId should be between 1000 and 1999");
        }
    }

    @Test
    void testGenerateRandomTemperatureInRangeAndPrecision() {
        for (int i = 0; i < 100; i++) {
            SensorData data = SensorData.generateRandom();            
            double temp = data.getTemperature();
            assertTrue(temp >= 5.0 && temp <= 30.0, "Temperature should be between 5.0 and 30.0");            // Check if rounded to 2 decimal places
            String[] parts = String.valueOf(temp).split("\\.");
            if (parts.length == 2) {
                assertTrue(parts[1].length() <= 2, "Temperature should have at most 2 decimal places");
            }
        }
    }

    @Test
    void testToStringFormat() {        
        SensorData data = SensorData.generateRandom();
        String result = data.toString();
        assertTrue(result.contains("Sensor ID: "), "String should contain 'Sensor ID: '");
        assertTrue(result.contains("Temperature: "), "String should contain 'Temperature: '");
    }

    @Test
    void testRoundMethod() {
        assertEquals(3.14, SensorData.round(3.14159, 2));
        assertEquals(3.0, SensorData.round(3.001, 0));
        assertThrows(IllegalArgumentException.class, () -> SensorData.round(3.14, -1));
    }
}