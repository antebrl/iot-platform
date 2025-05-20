package org.example.db;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.example.DatabaseServiceGrpc;
import org.example.Empty;
import org.example.Key;
import org.example.Response;
import org.example.CreateResponse;
import org.example.SensorDataProto;
import org.example.SensorDataWithId;
import org.example.SensorDataWithIdList;

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

    public CreateResponse create(SensorDataProto data) {
        return stub.create(data);
    }

    public SensorDataWithId read(String id) {
        Key key = Key.newBuilder().setId(id).build();
        return stub.read(key);
    }

    public Response update(SensorDataWithId dataWithId) {
        return stub.update(dataWithId);
    }

    public Response delete(String id) {
        Key key = Key.newBuilder().setId(id).build();
        return stub.delete(key);
    }

    public SensorDataWithIdList readAll() {
        Empty request = Empty.newBuilder().build();
        return stub.readAll(request);
    }
}