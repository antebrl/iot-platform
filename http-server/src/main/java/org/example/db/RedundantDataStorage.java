package org.example.db;

import org.example.SensorData;
import java.util.Objects;

public class RedundantDataStorage implements DataStorage {

    private final DataStorage primary;
    private final DataStorage backup;

    public RedundantDataStorage(DataStorage primary, DataStorage backup) {
        this.primary = Objects.requireNonNull(primary);
        this.backup = Objects.requireNonNull(backup);
    }

    @Override
    public boolean create(SensorData data) {
        boolean primarySuccess = primary.create(data);
        boolean backupSuccess = backup.create(data);
        return primarySuccess && backupSuccess;
    }

    @Override
    public String read(String id) {
        // Nur aus Primary lesen
        return primary.read(id);
    }

    @Override
    public boolean update(SensorData data) {
        boolean primarySuccess = primary.update(data);
        boolean backupSuccess = backup.update(data);
        return primarySuccess && backupSuccess;
    }

    @Override
    public boolean delete(String id) {
        boolean primarySuccess = primary.delete(id);
        boolean backupSuccess = backup.delete(id);
        return primarySuccess && backupSuccess;
    }

    @Override
    public String readAll() {
        // Nur aus Primary lesen
        return primary.readAll();
    }

    @Override
    public void clear() {
        primary.clear();
        backup.clear();
    }
}
