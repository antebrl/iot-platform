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
import org.example.UpdateRequest;
import org.example.DeleteRequest;

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
    public void update(UpdateRequest request, StreamObserver<CreateResponse> responseObserver) {
        String id = request.getId();
        SensorDataRequest updatedDataRequest = request.getUpdatedData();

        // Check if the entry exists
        if (db.containsKey(id)) {
            SensorDataStored updatedDataStored = SensorDataStored.newBuilder()
                    .setId(id) // Keep the existing ID
                    .setSensorId(updatedDataRequest.getSensorId())
                    .setTemperature(updatedDataRequest.getTemperature())
                    .build();
            db.put(id, updatedDataStored); // Replace the old entry

            CreateResponse response = CreateResponse.newBuilder()
                    .setId(id)
                    .setSuccess(true)
                    .setMessage("Entry updated with ID: " + id)
                    .build();
            responseObserver.onNext(response);
        } else {
            CreateResponse response = CreateResponse.newBuilder()
                    .setId(id)
                    .setSuccess(false)
                    .setMessage("Entry with ID: " + id + " not found.")
                    .build();
            responseObserver.onNext(response);
        }
        responseObserver.onCompleted();
    }

    @Override
    public void delete(DeleteRequest request, StreamObserver<CreateResponse> responseObserver) {
        String id = request.getId();
        SensorDataStored removedData = db.remove(id);

        boolean removed = removedData != null;
        String message = removed ? "Entry deleted with ID: " + id : "Entry with ID: " + id + " not found.";

        CreateResponse response = CreateResponse.newBuilder()
                .setId(id)
                .setSuccess(removed)
                .setMessage(message)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void readAll(Empty request, StreamObserver<SensorDataStoredList> responseObserver) {
        SensorDataStoredList.Builder listBuilder = SensorDataStoredList.newBuilder();
        db.values().forEach(listBuilder::addEntries);
        responseObserver.onNext(listBuilder.build());
        responseObserver.onCompleted();
    }
}
