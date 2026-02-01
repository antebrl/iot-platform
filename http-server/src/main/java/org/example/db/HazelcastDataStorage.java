package org.example.db;

import com.google.gson.Gson;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.IMap;
import org.example.SensorData;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class HazelcastDataStorage implements TwoPCDataStorage {
    private HazelcastInstance client;
    private final IMap<String, HazelcastJsonValue> map;
    private final Gson gson = new Gson();
    private final ConcurrentHashMap<String, TransactionData> preparedTransactions = new ConcurrentHashMap<>();
    
    private static class TransactionData {
        final String operation;
        final SensorData data;
        final HazelcastJsonValue originalValue; // For rollback
        
        TransactionData(String operation, SensorData data, HazelcastJsonValue originalValue) {
            this.operation = operation;
            this.data = data;
            this.originalValue = originalValue;
        }
    }

    public HazelcastDataStorage() {
        ClientConfig config = new ClientConfig();
        config.setClusterName("dev");
        config.setInstanceName("my-java-client");
        config.getNetworkConfig().addAddress("hazelcast-node:5701");

        int retryCount = 0;
        while (retryCount < 10) {
            try {
                client = HazelcastClient.newHazelcastClient(config);
                System.out.println("✅ Verbunden mit Hazelcast-Cluster: " + client.getCluster().getMembers());
                break;
            } catch (Exception e) {
                System.out.println("❌ Verbindung fehlgeschlagen, versuche erneut...");
                retryCount++;
                if (retryCount >= 10) {
                    throw new RuntimeException("Konnte keine Verbindung zum Hazelcast-Cluster herstellen.");
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (client != null) {
            this.map = client.getMap("sensorData");
            System.out.println("Initialisierte Map 'sensorData' mit Größe: " + map.size());
        } else {
            throw new RuntimeException("Hazelcast-Client konnte nicht initialisiert werden.");
        }
    }

    @Override
    public boolean create(SensorData data) {
        String jsonStr = gson.toJson(data);
        String key = data.getId();
        if (key == null || key.isEmpty()) {
            key = "sensor-" + data.getSensorId() + "-" + System.currentTimeMillis();
        }
        map.put(key, new HazelcastJsonValue(jsonStr));
        System.out.println("Daten geschrieben unter Key: " + key + ", aktuelle Map-Größe: " + map.size());
        return true;
    }

    @Override
    public String read(String id) {
        HazelcastJsonValue json = map.get(id);
        return json != null ? json.toString() : null;
    }

    @Override
    public boolean update(SensorData data) {
        String key = data.getId();
        if (key == null || !map.containsKey(key)) return false;
        map.put(key, new HazelcastJsonValue(gson.toJson(data)));
        return true;
    }

    @Override
    public boolean delete(String id) {
        return map.remove(id) != null;
    }

    @Override
    public String readAll() {
        return gson.toJson(
                map.values().stream()
                        .map(val -> gson.fromJson(val.toString(), Object.class))
                        .collect(Collectors.toList())
        );
    }

    @Override
    public void clear() {
        map.clear();
    }

    public HazelcastInstance getHazelcastInstance() {
        return client;
    }
    
    // 2PC Protocol Implementation
    @Override
    public boolean prepare(String transactionId, String operation, SensorData data) {
        try {
            HazelcastJsonValue originalValue = null;
            String key = data.getId();
            
            // Validate the operation can be performed
            switch (operation) {
                case "CREATE":
                    if (key == null || key.isEmpty()) {
                        key = "sensor-" + data.getSensorId() + "-" + System.currentTimeMillis();
                        data = SensorData.builder()
                                .id(key)
                                .sensorId(data.getSensorId())
                                .temperature(data.getTemperature())
                                .build();
                    }
                    if (map.containsKey(key)) {
                        return false; // Key already exists
                    }
                    break;
                    
                case "UPDATE":
                    if (key == null || !map.containsKey(key)) {
                        return false; // Key doesn't exist
                    }
                    originalValue = map.get(key);
                    break;
                    
                case "DELETE":
                    if (key == null || !map.containsKey(key)) {
                        return false; // Key doesn't exist
                    }
                    originalValue = map.get(key);
                    break;
                    
                default:
                    return false; // Unknown operation
            }
            
            // Store transaction data for commit/abort
            preparedTransactions.put(transactionId, new TransactionData(operation, data, originalValue));
            System.out.println("Hazelcast prepared transaction: " + transactionId + " for operation: " + operation);
            return true;
            
        } catch (Exception e) {
            System.err.println("Error preparing Hazelcast transaction: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean commit(String transactionId) {
        TransactionData txData = preparedTransactions.remove(transactionId);
        if (txData == null) {
            System.err.println("No prepared transaction found for ID: " + transactionId);
            return false;
        }
        
        try {
            String key = txData.data.getId();
            
            switch (txData.operation) {
                case "CREATE":
                    map.put(key, new HazelcastJsonValue(gson.toJson(txData.data)));
                    System.out.println("Hazelcast committed CREATE for key: " + key);
                    break;
                    
                case "UPDATE":
                    map.put(key, new HazelcastJsonValue(gson.toJson(txData.data)));
                    System.out.println("Hazelcast committed UPDATE for key: " + key);
                    break;
                    
                case "DELETE":
                    map.remove(key);
                    System.out.println("Hazelcast committed DELETE for key: " + key);
                    break;
                    
                default:
                    return false;
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Error committing Hazelcast transaction: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean abort(String transactionId) {
        TransactionData txData = preparedTransactions.remove(transactionId);
        if (txData == null) {
            System.err.println("No prepared transaction found for ID: " + transactionId);
            return false;
        }
        
        System.out.println("Hazelcast aborted transaction: " + transactionId);
        // No actual rollback needed since we didn't change anything yet
        return true;
    }
}
