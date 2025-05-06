package org.example;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

public class DataStorage {
    private final List<SensorData> entries = new ArrayList<>();
    private final Gson gson = new Gson();

    public synchronized void addFromJson(String json) {
        SensorData data = gson.fromJson(json, SensorData.class);
        entries.add(data);
    }

    public synchronized List<SensorData> getAll() {
        return new ArrayList<>(entries);
    }

    public synchronized String asText() {
        StringBuilder sb = new StringBuilder();
        for (SensorData data : entries) {
            sb.append(data).append("\n");
        }
        return sb.toString();
    }
}
