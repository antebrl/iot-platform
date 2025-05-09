package org.example;

import java.util.concurrent.ThreadLocalRandom;

public class SensorData {
    private int sensorId;
    private double temperature;

    public static SensorData generateRandom() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        SensorData data = new SensorData();
        data.sensorId = rnd.nextInt(1000, 2000);
        data.temperature = round(rnd.nextDouble(15, 30), 2);
        return data;
    }

    private static double round(double value, int places) {
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
        return "Sensor ID: " + sensorId + ", Temperatur: " + temperature;
    }
}
