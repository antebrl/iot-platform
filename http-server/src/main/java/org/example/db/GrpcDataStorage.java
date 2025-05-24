package org.example.db;

import com.google.gson.Gson;
import org.example.SensorData;
import org.example.SensorDataRequest;
import org.example.SensorDataStored;
import org.example.SensorDataStoredList;
import org.example.Response;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class GrpcDataStorage implements DataStorage {
    private final GrpcDatabaseClient grpcClient;
    private final Gson gson = new Gson();

    public GrpcDataStorage(String host, int port) {
        this.grpcClient = new GrpcDatabaseClient(host, port);
    }

    @Override
    public boolean create(SensorData data) {
        SensorDataRequest request = data.toGrpcRequest();
        Response response = grpcClient.create(request);
        return response.getSuccess();
    }

    @Override
    public String read(String id) {
        SensorDataStored dataStored = grpcClient.read(id);
        SensorData data = SensorData.fromGrpcStored(dataStored);
        return data != null ? gson.toJson(data) : null;
    }

    @Override
    public boolean update(SensorData data) {
         if (data.getId() == null || data.getId().isEmpty()) {
             System.err.println("Update failed: SensorData object is missing ID.");
             return false;
         }

         SensorDataRequest updatedDataRequest = data.toGrpcRequest();         
         Response response = grpcClient.update(data.getId(), updatedDataRequest);
         return response.getSuccess();
    }

    @Override
    public boolean delete(String id) {
        Response response = grpcClient.delete(id);
        return response.getSuccess();
    }

    @Override
    public String readAll() {
        SensorDataStoredList response = grpcClient.readAll();
        List<SensorDataStored> entries = response.getEntriesList();
        
        List<SensorData> sensorDataList = entries.stream()
                .map(SensorData::fromGrpcStored)
                .filter(data -> data != null)
                .collect(Collectors.toList());
        
        return gson.toJson(sensorDataList);
    }

    @Override
    public void clear() {
        // Get all entries first
        SensorDataStoredList response = grpcClient.readAll();
        List<SensorDataStored> entries = response.getEntriesList();
        
        // Delete each entry individually
        for (SensorDataStored entry : entries) {
            grpcClient.delete(entry.getId());
        }
    }
}