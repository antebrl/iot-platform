package org.example.db;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.example.DatabaseServiceGrpc;
import org.example.DataEntry;
import org.example.Key;
import org.example.Response;
import org.example.Empty;
import org.example.DataEntryList;
import org.example.SensorData;
import java.util.ArrayList;
import java.util.List;

public class GrpcDatabaseClient {
    private final DatabaseServiceGrpc.DatabaseServiceBlockingStub stub;

    public GrpcDatabaseClient(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        stub = DatabaseServiceGrpc.newBlockingStub(channel);
    }

    public boolean create(SensorData data) {
        DataEntry entry = DataEntry.newBuilder()
                .setId(String.valueOf(data.getSensorId()))
                .setValue(String.valueOf(data.getTemperature()))
                .build();
        Response response = stub.create(entry);
        return response.getSuccess();
    }

    public SensorData read(String id) {
        Key key = Key.newBuilder().setId(id).build();
        DataEntry entry = stub.read(key);
        if (entry.getId().isEmpty()) return null; // Assuming empty ID means not found
        SensorData data = new SensorData();
        // Add proper error handling for parsing if needed
        try {
            data.setSensorId(Integer.parseInt(entry.getId()));
            data.setTemperature(Double.parseDouble(entry.getValue()));
        } catch (NumberFormatException e) {
            System.err.println("Error parsing data from gRPC: " + e.getMessage());
            return null; // Or handle error appropriately
        }
        return data;
    }

    public boolean update(SensorData data) {
        DataEntry entry = DataEntry.newBuilder()
                .setId(String.valueOf(data.getSensorId()))
                .setValue(String.valueOf(data.getTemperature()))
                .build();
        Response response = stub.update(entry);
        return response.getSuccess();
    }

    public boolean delete(String id) {
        Key key = Key.newBuilder().setId(id).build();
        Response response = stub.delete(key);
        return response.getSuccess();
    }

    public List<SensorData> readAll() {
        Empty request = Empty.newBuilder().build();
        DataEntryList response = stub.readAll(request);
        List<SensorData> dataList = new ArrayList<>();
        for (DataEntry entry : response.getEntriesList()) {
            SensorData data = new SensorData();
             try {
                data.setSensorId(Integer.parseInt(entry.getId()));
                data.setTemperature(Double.parseDouble(entry.getValue()));
                dataList.add(data);
            } catch (NumberFormatException e) {
                System.err.println("Error parsing data from gRPC for readAll: " + e.getMessage());
                // Optionally skip or log problematic entries
            }
        }
        return dataList;
    }
}