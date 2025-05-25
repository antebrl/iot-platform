package org.example.db;

import com.google.gson.Gson;
import org.example.SensorData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InMemoryDataStorage implements DataStorage {
    private final List<SensorData> entries = new ArrayList<>();
    private final Gson gson = new Gson();    @Override
    public synchronized boolean create(SensorData data) {
        // Create a new entry with a generated UUID
        SensorData newData = SensorData.builder()
                .id(UUID.randomUUID().toString())
                .sensorId(data.getSensorId())
                .temperature(data.getTemperature())
                .build();
        entries.add(newData);
        return true;
    }

    @Override
    public synchronized String read(String id) {
        for (SensorData data : entries) {
            if (data.getId().equals(id)) {
                return gson.toJson(data);
            }
        }
        return null;
    }

    @Override
    public synchronized boolean update(SensorData data) {
        if (data.getId() == null || data.getId().isEmpty()) {
            return false;
        }

        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getId().equals(data.getId())) {
                entries.set(i, data);
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean delete(String id) {
        return entries.removeIf(d -> d.getId().equals(id));
    }    @Override
    public synchronized String readAll() {
        return gson.toJson(entries);
    }

    @Override
    public synchronized void clear() {
        entries.clear();
    }
}