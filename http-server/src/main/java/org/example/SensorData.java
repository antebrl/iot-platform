package org.example;

public class SensorData {
    private int sensorId;
    private double temperature;

    public int getSensorId() { return sensorId; }
    public double getTemperature() { return temperature; }

    @Override
    public String toString() {
        return "Sensor ID: " + sensorId + ", Temperatur: " + temperature;
    }
}
