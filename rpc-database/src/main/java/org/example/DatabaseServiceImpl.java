package org.example;

import io.grpc.stub.StreamObserver;
import java.util.concurrent.ConcurrentHashMap;
import org.example.DatabaseServiceGrpc;
import org.example.DataEntry;
import org.example.Key;
import org.example.Response;

public class DatabaseServiceImpl extends DatabaseServiceGrpc.DatabaseServiceImplBase {

    private final ConcurrentHashMap<String, String> db = new ConcurrentHashMap<>();

    @Override
    public void create(DataEntry request, StreamObserver<Response> responseObserver) {
        boolean inserted = db.putIfAbsent(request.getId(), request.getValue()) == null;
        sendResponse(responseObserver, inserted,
                inserted ? "Entry created." : "Entry already exists.");
    }

    @Override
    public void read(Key request, StreamObserver<DataEntry> responseObserver) {
        String val = db.get(request.getId());
        if (val != null) {
            responseObserver.onNext(
                    DataEntry.newBuilder().setId(request.getId()).setValue(val).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void update(DataEntry request, StreamObserver<Response> responseObserver) {
        boolean updated = db.replace(request.getId(), request.getValue()) != null;
        sendResponse(responseObserver, updated,
                updated ? "Entry updated." : "Entry not found.");
    }

    @Override
    public void delete(Key request, StreamObserver<Response> responseObserver) {
        boolean removed = db.remove(request.getId()) != null;
        sendResponse(responseObserver, removed,
                removed ? "Entry deleted." : "Entry not found.");
    }

    private void sendResponse(StreamObserver<Response> obs, boolean ok, String msg) {
        obs.onNext(Response.newBuilder().setSuccess(ok).setMessage(msg).build());
        obs.onCompleted();
    }
}
