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
    private final HazelcastInstance client;
    private final IMap<String, HazelcastJsonValue> map;
    private final Gson gson = new Gson();

    public HazelcastDataStorage() {
        ClientConfig config = new ClientConfig();
        config.setClusterName("dev");
        config.setInstanceName("my-java-client");
        config.getNetworkConfig().addAddress("hazelcast-node:5701");

        this.client = HazelcastClient.newHazelcastClient(config);
        System.out.println("✅ Verbunden mit Hazelcast-Cluster: " + client.getCluster().getMembers());

        this.map = client.getMap("sensorData");

        int sizeBefore = map.size();

        System.out.println("🧹 Lösche alte Map-Einträge aus 'sensorData'...");
        map.clear();
        System.out.println("🧹 sensorData Map gelöscht. Vorher: " + sizeBefore + " Einträge.");
    }

    public HazelcastDataStorage(IMap<String, HazelcastJsonValue> map) {
        this.client = null;
        this.map = map;
    }

    @Override
    public boolean create(SensorData data) {
        String jsonStr = gson.toJson(data);
        HazelcastJsonValue jsonValue = new HazelcastJsonValue(jsonStr);

        String key = data.getId();
        if (key == null || key.isEmpty()) {
            key = "sensor-" + data.getSensorId() + "-" + System.currentTimeMillis();
        }

        map.put(key, jsonValue);
        System.out.println("📦 Gespeichert mit Key=" + key + ": " + jsonStr);
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
        HazelcastJsonValue jsonValue = new HazelcastJsonValue(gson.toJson(data));
        map.put(key, jsonValue);
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
                        .map(val -> gson.fromJson(val.toString(), Object.class)) // wichtig!
                        .collect(Collectors.toList())
        );
    }


    @Override
    public void clear() {
        map.clear();
    }
}
