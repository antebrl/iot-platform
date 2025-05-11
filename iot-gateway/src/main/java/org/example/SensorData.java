package org.example;

import java.util.concurrent.ThreadLocalRandom;

public class SensorData {
    private int sensorId;
    private double temperature;
    
    // Predefined list of sensor IDs
    private static final int[] SENSOR_IDS = {1001, 1002, 1003, 1004, 1005, 1234, 1567, 1890};

    public static SensorData generateRandom() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        SensorData data = new SensorData();
        // Select a random sensor ID from the predefined list
        data.sensorId = SENSOR_IDS[rnd.nextInt(SENSOR_IDS.length)];
        data.temperature = round(rnd.nextDouble(5, 30), 2);
        return data;
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
