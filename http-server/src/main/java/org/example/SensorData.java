package org.example;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

public class SensorData {
    private int sensorIdTemp;
    private double temperature;
    private int sensorIdHumid;
    private int humidity;
    private int sensorIdPress;
    private double pressure;
    private int sensorIdCO2;
    private int co2;

    // Konstruktor
    public SensorData(int sensorIdTemp, double temperature,
                      int sensorIdHumid, int humidity,
                      int sensorIdPress, double pressure,
                      int sensorIdCO2, int co2) {
        this.sensorIdTemp = sensorIdTemp;
        this.temperature = temperature;
        this.sensorIdHumid = sensorIdHumid;
        this.humidity = humidity;
        this.sensorIdPress = sensorIdPress;
        this.pressure = pressure;
        this.sensorIdCO2 = sensorIdCO2;
        this.co2 = co2;
    }

    // Factory-Methode zur Erzeugung zufälliger Daten
    public static SensorData generateRandom() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        double temperature = round(rnd.nextDouble(15, 30), 2);
        int humidity = rnd.nextInt(30, 80);
        double pressure = round(rnd.nextDouble(980, 1050), 2);
        int co2 = rnd.nextInt(400, 1000);

        int sensorIdTemp = rnd.nextInt(1000, 2000);
        int sensorIdHumid = rnd.nextInt(2000, 3000);
        int sensorIdPress = rnd.nextInt(3000, 4000);
        int sensorIdCO2 = rnd.nextInt(4000, 5000);

        return new SensorData(sensorIdTemp, temperature, sensorIdHumid, humidity,
                sensorIdPress, pressure, sensorIdCO2, co2);
    }

    private static double round(double value, int places) {
        return new BigDecimal(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }

    // JSON-Darstellung
    public String toJson() {
        return String.format(
                "{\"sensorIdTemp\":%d,\"temperature\":%.2f," +
                        "\"sensorIdHumid\":%d,\"humidity\":%d," +
                        "\"sensorIdPress\":%d,\"pressure\":%.2f," +
                        "\"sensorIdCO2\":%d,\"co2\":%d}",
                sensorIdTemp, temperature,
                sensorIdHumid, humidity,
                sensorIdPress, pressure,
                sensorIdCO2, co2
        );
    }

    // Lesbare Ausgabe für Konsole
    @Override
    public String toString() {
        return String.format(
                "Temperatursensor ID: %d, Temperatur: %.2f °C%n" +
                        "Luftfeuchtigkeitssensor ID: %d, Feuchtigkeit: %d %% %n" +
                        "Luftdrucksensor ID: %d, Luftdruck: %.2f hPa%n" +
                        "CO₂-Sensor ID: %d, CO₂: %d ppm",
                sensorIdTemp, temperature,
                sensorIdHumid, humidity,
                sensorIdPress, pressure,
                sensorIdCO2, co2
        );
    }
    public double getTemperature() { return temperature; }
    public int getHumidity() { return humidity; }
    public double getPressure() { return pressure; }
    public int getCo2() { return co2; }

    public int getSensorIdTemp() { return sensorIdTemp; }
    public int getSensorIdHumid() { return sensorIdHumid; }
    public int getSensorIdPress() { return sensorIdPress; }
    public int getSensorIdCO2() { return sensorIdCO2; }
}