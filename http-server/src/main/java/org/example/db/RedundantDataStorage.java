package org.example.db;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.transaction.TransactionContext;
import com.hazelcast.transaction.TransactionOptions;
import com.hazelcast.transaction.TransactionalMap;
import org.example.SensorData;

import java.util.Objects;

public class RedundantDataStorage implements DataStorage {

    private final DataStorage primary;
    private final HazelcastInstance hazelcast;
    private final Gson gson = new Gson();

    public RedundantDataStorage(DataStorage primary, HazelcastInstance hazelcast) {
        this.primary = Objects.requireNonNull(primary);
        this.hazelcast = Objects.requireNonNull(hazelcast);
    }

    @Override
    public boolean create(SensorData data) {
        TransactionOptions options = new TransactionOptions()
                .setTransactionType(TransactionOptions.TransactionType.TWO_PHASE);

        TransactionContext context = hazelcast.newTransactionContext(options);
        context.beginTransaction();

        try {
            boolean primarySuccess = primary.create(data);
            if (!primarySuccess) {
                context.rollbackTransaction();
                return false;
            }

            TransactionalMap<String, HazelcastJsonValue> txMap = context.getMap("sensorData");
            String key = data.getId();
            if (key == null || key.isEmpty()) {
                key = "sensor-" + data.getSensorId() + "-" + System.currentTimeMillis();
            }
            txMap.put(key, new HazelcastJsonValue(gson.toJson(data)));

            context.commitTransaction();
            return true;
        } catch (Exception e) {
            context.rollbackTransaction();
            System.err.println("❌ Fehler beim 2PC-Speichern: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String read(String id) {
        return primary.read(id);
    }

    @Override
    public boolean update(SensorData data) {
        boolean primarySuccess = primary.update(data);
        if (!primarySuccess) return false;

        TransactionOptions options = new TransactionOptions()
                .setTransactionType(TransactionOptions.TransactionType.TWO_PHASE);
        TransactionContext context = hazelcast.newTransactionContext(options);
        context.beginTransaction();

        try {
            TransactionalMap<String, HazelcastJsonValue> txMap = context.getMap("sensorData");
            txMap.put(data.getId(), new HazelcastJsonValue(gson.toJson(data)));
            context.commitTransaction();
            return true;
        } catch (Exception e) {
            context.rollbackTransaction();
            System.err.println("❌ Fehler beim Update (Hazelcast): " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(String id) {
        boolean primarySuccess = primary.delete(id);
        if (!primarySuccess) return false;

        TransactionOptions options = new TransactionOptions()
                .setTransactionType(TransactionOptions.TransactionType.TWO_PHASE);
        TransactionContext context = hazelcast.newTransactionContext(options);
        context.beginTransaction();

        try {
            TransactionalMap<String, HazelcastJsonValue> txMap = context.getMap("sensorData");
            txMap.remove(id);
            context.commitTransaction();
            return true;
        } catch (Exception e) {
            context.rollbackTransaction();
            System.err.println("❌ Fehler beim Delete (Hazelcast): " + e.getMessage());
            return false;
        }
    }

    @Override
    public String readAll() {
        return primary.readAll();
    }

    @Override
    public void clear() {
        primary.clear();
        hazelcast.getMap("sensorData").clear();
    }
}
