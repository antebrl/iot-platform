package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SensorDataTest {

    @Test
    public void testGeneratedValuesWithinRange() {
        SensorData data = SensorData.generateRandom();

        assertTrue(data.getTemperature() >= 15 && data.getTemperature() <= 30, "Temperature out of range");
        assertTrue(data.getHumidity() >= 30 && data.getHumidity() < 80, "Humidity out of range");
        assertTrue(data.getPressure() >= 980 && data.getPressure() <= 1050, "Pressure out of range");
        assertTrue(data.getCo2() >= 400 && data.getCo2() < 1000, "CO₂ out of range");

        assertTrue(data.getSensorIdTemp() >= 1000 && data.getSensorIdTemp() < 2000);
        assertTrue(data.getSensorIdHumid() >= 2000 && data.getSensorIdHumid() < 3000);
        assertTrue(data.getSensorIdPress() >= 3000 && data.getSensorIdPress() < 4000);
        assertTrue(data.getSensorIdCO2() >= 4000 && data.getSensorIdCO2() < 5000);
    }

    @Test
    public void testJsonFormatNotEmpty() {
        SensorData data = SensorData.generateRandom();
        String json = data.toJson();
        assertNotNull(json);
        assertTrue(json.contains("\"temperature\""));
        assertTrue(json.contains("\"humidity\""));
        assertTrue(json.contains("\"pressure\""));
        assertTrue(json.contains("\"co2\""));
    }

    @Test
    public void testToStringContainsKeywords() {
        SensorData data = SensorData.generateRandom();
        String output = data.toString();
        assertTrue(output.contains("Temperatursensor"));
        assertTrue(output.contains("Luftfeuchtigkeitssensor"));
        assertTrue(output.contains("Luftdrucksensor"));
        assertTrue(output.contains("CO₂-Sensor"));
    }
}
