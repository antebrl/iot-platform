package org.example;

/**
 * Represents sensor data with ID and temperature reading for HTTP and InMemory storage.
 */
public class SensorData {
    private int sensorId;
    private double temperature;

    // Getters
    public int getSensorId() { return sensorId; }
    public double getTemperature() { return temperature; }

    // Setters (might be used by Gson for deserialization)
    public void setSensorId(int sensorId) { this.sensorId = sensorId; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    @Override
    public String toString() {
        return "Sensor ID: " + sensorId + ", Temperatur: " + temperature;
    }
} 