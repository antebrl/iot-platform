package org.example;

import io.grpc.stub.StreamObserver;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import org.example.DatabaseServiceGrpc;
import org.example.Empty;
import org.example.Key;
import org.example.Response;
import org.example.CreateResponse;
import org.example.SensorDataRequest;
import org.example.SensorDataStored;
import org.example.SensorDataStoredList;

public class DatabaseServiceImpl extends DatabaseServiceGrpc.DatabaseServiceImplBase {

    private final ConcurrentHashMap<String, SensorDataStored> db = new ConcurrentHashMap<>();

    @Override
    public void create(SensorDataRequest request, StreamObserver<CreateResponse> responseObserver) {
        String id = UUID.randomUUID().toString();
        SensorDataStored dataToStore = SensorDataStored.newBuilder()
                .setId(id)
                .setSensorId(request.getSensorId())
                .setTemperature(request.getTemperature())
                .build();

        boolean inserted = db.putIfAbsent(id, dataToStore) == null;

        CreateResponse response = CreateResponse.newBuilder()
                .setId(id)
                .setSuccess(inserted)
                .setMessage(inserted ? "Entry created with ID: " + id : "Entry creation failed.")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void read(Key request, StreamObserver<SensorDataStored> responseObserver) {
        SensorDataStored data = db.get(request.getId());
        if (data != null) {
            responseObserver.onNext(data);
        }
        responseObserver.onCompleted();
    }

    @Override
    public void update(SensorDataStored request, StreamObserver<Response> responseObserver) {
        String id = request.getId();
        // Only update if the entry exists
        boolean updated = db.replace(id, request) != null;
        sendResponse(responseObserver, updated,
                updated ? "Entry updated." : "Entry not found.");
    }

    @Override
    public void delete(Key request, StreamObserver<Response> responseObserver) {
        boolean removed = db.remove(request.getId()) != null;
        sendResponse(responseObserver, removed,
                removed ? "Entry deleted." : "Entry not found.");
    }

    @Override
    public void readAll(Empty request, StreamObserver<SensorDataStoredList> responseObserver) {
        SensorDataStoredList.Builder listBuilder = SensorDataStoredList.newBuilder();
        db.values().forEach(listBuilder::addEntries);
        responseObserver.onNext(listBuilder.build());
        responseObserver.onCompleted();
    }

    private void sendResponse(StreamObserver<Response> obs, boolean ok, String msg) {
        obs.onNext(Response.newBuilder().setSuccess(ok).setMessage(msg).build());
        obs.onCompleted();
    }
}
