package org.example;

import java.util.concurrent.ThreadLocalRandom;

public class SensorData {
    private int sensorId;
    private double temperature;
    
    public SensorData(int sensorId) {
        this.sensorId = sensorId;
    }

    public static SensorData generateRandom(int sensorId) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        SensorData data = new SensorData(sensorId);
        data.temperature = round(rnd.nextDouble(5, 30), 2);
        return data;
    }

    public static int getRandomSensorId() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        return rnd.nextInt(1, 1001); // Random number between 1 and 1000 inclusive
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public int getSensorId() { return sensorId; }
    public double getTemperature() { return temperature; }

    @Override
    public String toString() {
        return "Sensor ID: " + sensorId + ", Temperature: " + temperature;
    }
}
