package org.example.db;

import com.google.gson.Gson;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.IMap;
import org.example.SensorData;

import java.util.stream.Collectors;

public class HazelcastDataStorage implements DataStorage {
    private HazelcastInstance client;
    private final IMap<String, HazelcastJsonValue> map;
    private final Gson gson = new Gson();

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
            // Entferne map.clear(), damit die Daten nicht beim Start gelöscht werden
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
}
