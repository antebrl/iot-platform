package org.example.db;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.example.*;

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

    public Response create(SensorDataRequest data) {
        return stub.create(data);
    }

    public SensorDataStored read(String id) {
        Key key = Key.newBuilder().setId(id).build();
        return stub.read(key);
    }

    public Response update(String id, SensorDataRequest updatedData) {
        UpdateRequest request = UpdateRequest.newBuilder()
                .setId(id)
                .setUpdatedData(updatedData)
                .build();
        return stub.update(request);
    }

    public Response delete(String id) {
        DeleteRequest request = DeleteRequest.newBuilder()
                .setId(id)
                .build();
        return stub.delete(request);
    }

    public SensorDataStoredList readAll() {
        Empty request = Empty.newBuilder().build();
        return stub.readAll(request);
    }
    
    // 2PC Protocol methods
    public PrepareResponse prepare(TransactionRequest request) {
        return stub.prepare(request);
    }
    
    public Response commit(TransactionId transactionId) {
        return stub.commit(transactionId);
    }
    
    public Response abort(TransactionId transactionId) {
        return stub.abort(transactionId);
    }
}