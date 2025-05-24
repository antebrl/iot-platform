package org.example;

/**
 * Represents sensor data with ID and temperature reading for HTTP and InMemory storage.
 */
public class SensorData {
    private String id;
    private int sensorId;
    private double temperature;

    // Private constructor to enforce the use of the Builder
    private SensorData() {}

    // Getters
    public String getId() { return id; }
    public int getSensorId() { return sensorId; }
    public double getTemperature() { return temperature; }

    // Setters (might be used by Gson for deserialization)
    public void setSensorId(int sensorId) { this.sensorId = sensorId; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    @Override
    public String toString() {
        return "ID: " + id + ", Sensor ID: " + sensorId + ", Temperatur: " + temperature;
    }

    // Builder Klasse
    public static class Builder {
        private String id;
        private int sensorId;
        private double temperature;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder sensorId(int sensorId) {
            this.sensorId = sensorId;
            return this;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public SensorData build() {
            SensorData data = new SensorData();
            data.id = this.id;
            data.sensorId = this.sensorId;
            data.temperature = this.temperature;
            return data;
        }
    }

    // Statische Methode, um Builder zu erhalten
    public static Builder builder() {
        return new Builder();
    }
} 