package org.example.db;

import com.google.gson.Gson;
import org.example.SensorData;
import java.util.ArrayList;
import java.util.List;

public class InMemoryDataStorage implements DataStorage {
    private final List<SensorData> entries = new ArrayList<>();
    private final Gson gson = new Gson();

    @Override
    public synchronized boolean create(SensorData data) {
        entries.add(data);
        return true;
    }

    @Override
    public synchronized String read(String id) {
        for (SensorData data : entries) {
            if (String.valueOf(data.getSensorId()).equals(id)) {
                return gson.toJson(data);
            }
        }
        return null;
    }

    @Override
    public synchronized boolean update(SensorData data) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getSensorId() == data.getSensorId()) {
                entries.set(i, data);
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean delete(String id) {
        return entries.removeIf(d -> String.valueOf(d.getSensorId()).equals(id));
    }

    @Override
    public synchronized String readAll() {
        return gson.toJson(entries);
    }
} 