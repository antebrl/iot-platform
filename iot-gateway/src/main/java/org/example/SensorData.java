package org.example;

import java.util.concurrent.ThreadLocalRandom;

public class SensorData {
    private String id;
    private int sensorId;
    private double temperature;
    private long timestamp;

    // Predefined list of sensor IDs
    private static final int[] SENSOR_IDS = {1001, 1002, 1003, 1004, 1005, 1234, 1567, 1890};

    public static SensorData generateRandom() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int sensorId = SENSOR_IDS[rnd.nextInt(SENSOR_IDS.length)];
        double temperature = round(rnd.nextDouble(5, 30), 2);
        long timestamp = System.currentTimeMillis();
        String id = "sensor-" + sensorId + "-" + timestamp;

        SensorData data = new SensorData();
        data.setId(id);
        data.setSensorId(sensorId);
        data.setTemperature(temperature);
        data.setTimestamp(timestamp);
        return data;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    // Getter
    public String getId() {
        return id;
    }

    public int getSensorId() {
        return sensorId;
    }

    public double getTemperature() {
        return temperature;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Setter
    public void setId(String id) {
        this.id = id;
    }

    public void setSensorId(int sensorId) {
        this.sensorId = sensorId;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ID: " + id +
                ", Sensor ID: " + sensorId +
                ", Temperature: " + temperature +
                ", Timestamp: " + timestamp;
    }
}
