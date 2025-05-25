package org.example;

import io.grpc.stub.StreamObserver;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class DatabaseServiceImpl extends DatabaseServiceGrpc.DatabaseServiceImplBase {

    private final ConcurrentHashMap<String, SensorDataStored> db = new ConcurrentHashMap<>();
    private final List<String> insertionOrder = new CopyOnWriteArrayList<>();

    @Override
    public void create(SensorDataRequest request, StreamObserver<Response> responseObserver) {
        //System.out.println("Received create request: " + request);
        if (request.getTemperature() == null || request.getTemperature().trim().isEmpty()) {
            Response response = Response.newBuilder()
                    .setId("")
                    .setSuccess(false)
                    .setMessage("Temperature must not be empty.")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        String id = UUID.randomUUID().toString();
        SensorDataStored dataToStore = SensorDataStored.newBuilder()
                .setId(id)
                .setSensorId(request.getSensorId())
                .setTemperature(request.getTemperature())
                .build();

        boolean inserted = db.putIfAbsent(id, dataToStore) == null;
        if (inserted) {
            insertionOrder.add(id);
        }

        Response response = Response.newBuilder()
                .setId(id)
                .setSuccess(inserted)
                .setMessage(inserted ? "Entry created with ID: " + id : "Entry creation failed.")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void read(Key request, StreamObserver<SensorDataStored> responseObserver) {
        //System.out.println("Received read request for ID: " + request.getId());
        SensorDataStored data = db.get(request.getId());
        if (data != null) {
            responseObserver.onNext(data);
        } else {
            responseObserver.onNext(SensorDataStored.newBuilder().build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void update(UpdateRequest request, StreamObserver<Response> responseObserver) {
        //System.out.println("Received update request for ID: " + request.getId());
        String id = request.getId();
        SensorDataRequest updatedDataRequest = request.getUpdatedData();

        if (db.containsKey(id)) {
            SensorDataStored updatedDataStored = SensorDataStored.newBuilder()
                    .setId(id)
                    .setSensorId(updatedDataRequest.getSensorId())
                    .setTemperature(updatedDataRequest.getTemperature())
                    .build();
            db.put(id, updatedDataStored);

            Response response = Response.newBuilder()
                    .setId(id)
                    .setSuccess(true)
                    .setMessage("Entry updated with ID: " + id)
                    .build();
            responseObserver.onNext(response);
        } else {
            Response response = Response.newBuilder()
                    .setId(id)
                    .setSuccess(false)
                    .setMessage("Entry with ID: " + id + " not found.")
                    .build();
            responseObserver.onNext(response);
        }
        responseObserver.onCompleted();
    }

    @Override
    public void delete(DeleteRequest request, StreamObserver<Response> responseObserver) {
        //System.out.println("Received delete request for ID: " + request.getId());
        String id = request.getId();
        SensorDataStored removedData = db.remove(id);

        boolean removed = removedData != null;
        if (removed) {
            insertionOrder.remove(id);
        }

        String message = removed ? "Entry deleted with ID: " + id : "Entry with ID: " + id + " not found.";

        Response response = Response.newBuilder()
                .setId(id)
                .setSuccess(removed)
                .setMessage(message)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void readAll(Empty request, StreamObserver<SensorDataStoredList> responseObserver) {
        //System.out.println("Received readAll request");
        SensorDataStoredList.Builder listBuilder = SensorDataStoredList.newBuilder();
        for (String id : insertionOrder) {
            SensorDataStored entry = db.get(id);
            if (entry != null) {
                listBuilder.addEntries(entry);
            }
        }
        responseObserver.onNext(listBuilder.build());
        responseObserver.onCompleted();
    }
}
