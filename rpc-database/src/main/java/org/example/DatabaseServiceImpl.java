package org.example;

import io.grpc.stub.StreamObserver;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import org.example.DatabaseServiceGrpc;
import org.example.Empty;
import org.example.Key;
import org.example.Response;
import org.example.CreateResponse;
import org.example.SensorDataProto;
import org.example.SensorDataWithId;
import org.example.SensorDataWithIdList;

public class DatabaseServiceImpl extends DatabaseServiceGrpc.DatabaseServiceImplBase {

    private final ConcurrentHashMap<String, SensorDataProto> db = new ConcurrentHashMap<>();

    @Override
    public void create(SensorDataProto request, StreamObserver<CreateResponse> responseObserver) {
        String id = UUID.randomUUID().toString();
        SensorDataProto dataToStore = SensorDataProto.newBuilder()
                .setValue(request.getValue())
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
    public void read(Key request, StreamObserver<SensorDataWithId> responseObserver) {
        SensorDataProto data = db.get(request.getId());
        if (data != null) {
            responseObserver.onNext(
                    SensorDataWithId.newBuilder()
                            .setId(request.getId())
                            .setData(data)
                            .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void update(SensorDataWithId request, StreamObserver<Response> responseObserver) {
        String id = request.getId();
        SensorDataProto newData = request.getData();
        boolean updated = db.replace(id, newData) != null;
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
    public void readAll(Empty request, StreamObserver<SensorDataWithIdList> responseObserver) {
        SensorDataWithIdList.Builder listBuilder = SensorDataWithIdList.newBuilder();
        db.forEach((id, data) -> listBuilder.addEntries(
                SensorDataWithId.newBuilder().setId(id).setData(data).build()
        ));
        responseObserver.onNext(listBuilder.build());
        responseObserver.onCompleted();
    }

    private void sendResponse(StreamObserver<Response> obs, boolean ok, String msg) {
        obs.onNext(Response.newBuilder().setSuccess(ok).setMessage(msg).build());
        obs.onCompleted();
    }
}
