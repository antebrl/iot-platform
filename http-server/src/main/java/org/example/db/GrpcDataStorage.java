package org.example.db;

import com.google.gson.Gson;
import org.example.SensorData;
import java.util.Collections;
import java.util.List;

public class GrpcDataStorage implements DataStorage {
    private final GrpcDatabaseClient grpcClient;
    private final Gson gson = new Gson();

    public GrpcDataStorage(String host, int port) {
        this.grpcClient = new GrpcDatabaseClient(host, port);
    }

    @Override
    public boolean create(SensorData data) {
        return grpcClient.create(data);
    }

    @Override
    public String read(String id) {
        SensorData data = grpcClient.read(id);
        return data != null ? gson.toJson(data) : null;
    }

    @Override
    public boolean update(SensorData data) {
        return grpcClient.update(data);
    }

    @Override
    public boolean delete(String id) {
        return grpcClient.delete(id);
    }

    @Override
    public String readAll() {
        List<SensorData> all = grpcClient.readAll();
        return gson.toJson(all);
    }
} 