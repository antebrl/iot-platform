package org.example.db;

import org.example.SensorData;
import java.util.List;

public interface DataStorage {
    boolean create(SensorData data);
    String read(String id);
    boolean update(SensorData data);
    boolean delete(String id);
    String readAll();
    void clear();
}