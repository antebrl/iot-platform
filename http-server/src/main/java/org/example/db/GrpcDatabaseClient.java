package org.example.db;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.example.DatabaseServiceGrpc;
import org.example.Empty;
import org.example.Key;
import org.example.Response;
import org.example.CreateResponse;
import org.example.SensorDataRequest;
import org.example.SensorDataStored;
import org.example.SensorDataStoredList;
import org.example.UpdateRequest;
import org.example.DeleteRequest;

import java.util.List;
import java.util.ArrayList;

public class GrpcDatabaseClient {
    private final DatabaseServiceGrpc.DatabaseServiceBlockingStub stub;

    public GrpcDatabaseClient(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        stub = DatabaseServiceGrpc.newBlockingStub(channel);
    }

    public CreateResponse create(SensorDataRequest data) {
        return stub.create(data);
    }

    public SensorDataStored read(String id) {
        Key key = Key.newBuilder().setId(id).build();
        return stub.read(key);
    }

    public CreateResponse update(String id, SensorDataRequest updatedData) {
        UpdateRequest request = UpdateRequest.newBuilder()
                .setId(id)
                .setUpdatedData(updatedData)
                .build();
        return stub.update(request);
    }

    public CreateResponse delete(String id) {
        DeleteRequest request = DeleteRequest.newBuilder()
                .setId(id)
                .build();
        return stub.delete(request);
    }

    public SensorDataStoredList readAll() {
        Empty request = Empty.newBuilder().build();
        return stub.readAll(request);
    }
}