package org.example.db;

import com.google.gson.Gson;
import org.example.SensorData;
import org.example.SensorDataRequest;
import org.example.SensorDataStored;
import org.example.SensorDataStoredList;
import org.example.CreateResponse;
import java.util.List;
import java.util.ArrayList;

public class GrpcDataStorage implements DataStorage {
    private final GrpcDatabaseClient grpcClient;
    private final Gson gson = new Gson();

    public GrpcDataStorage(String host, int port) {
        this.grpcClient = new GrpcDatabaseClient(host, port);
    }

    @Override
    public boolean create(SensorData data) {
        SensorDataRequest request = SensorDataRequest.newBuilder()
                .setSensorId(data.getSensorId())
                .setTemperature(String.valueOf(data.getTemperature()))
                .build();
        CreateResponse response = grpcClient.create(request);
        return response.getSuccess();
    }

    @Override
    public String read(String id) {
        SensorDataStored dataStored = grpcClient.read(id);
        if (dataStored != null && !dataStored.getId().isEmpty()) {
            try {
                org.example.SensorData data = org.example.SensorData.builder()
                        .id(dataStored.getId())
                        .sensorId(dataStored.getSensorId())
                        .temperature(Double.parseDouble(dataStored.getTemperature()))
                        .build();
                return gson.toJson(data);
            } catch (NumberFormatException e) {
                System.err.println("Error parsing temperature from gRPC data in read: " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    @Override
    public boolean update(SensorData data) {
         if (data.getId() == null || data.getId().isEmpty()) {
             System.err.println("Update failed: SensorData object is missing ID.");
             return false;
         }

         SensorDataRequest updatedDataRequest = SensorDataRequest.newBuilder()
                 .setSensorId(data.getSensorId())
                 .setTemperature(String.valueOf(data.getTemperature()))
                 .build();

         CreateResponse response = grpcClient.update(data.getId(), updatedDataRequest);
         return response.getSuccess();
    }

    @Override
    public boolean delete(String id) {
        CreateResponse response = grpcClient.delete(id);
        return response.getSuccess();
    }

    @Override
    public String readAll() {
        SensorDataStoredList response = grpcClient.readAll();
        List<SensorDataStored> entries = response.getEntriesList();
        List<org.example.SensorData> sensorDataList = new java.util.ArrayList<>();

        for (SensorDataStored entry : entries) {
            try {
                org.example.SensorData data = org.example.SensorData.builder()
                        .id(entry.getId())
                        .sensorId(entry.getSensorId())
                        .temperature(Double.parseDouble(entry.getTemperature()))
                        .build();
                sensorDataList.add(data);
            } catch (NumberFormatException e) {
                System.err.println("Error parsing temperature from gRPC data in readAll: " + e.getMessage());
                // Fehlerhaften Eintrag überspringen oder loggen
            }
        }

        return gson.toJson(sensorDataList);
    }
} 