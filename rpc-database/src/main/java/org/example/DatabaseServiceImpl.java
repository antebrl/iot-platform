package org.example;

import io.grpc.stub.StreamObserver;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class DatabaseServiceImpl extends DatabaseServiceGrpc.DatabaseServiceImplBase {

    private final ConcurrentHashMap<String, SensorDataStored> db = new ConcurrentHashMap<>();
    private final List<String> insertionOrder = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, TransactionData> preparedTransactions = new ConcurrentHashMap<>();
    
    private static class TransactionData {
        final String operation;
        final SensorDataStored data;
        final SensorDataStored originalValue; // For rollback
        
        TransactionData(String operation, SensorDataStored data, SensorDataStored originalValue) {
            this.operation = operation;
            this.data = data;
            this.originalValue = originalValue;
        }
    }

    @Override
    public void create(SensorDataRequest request, StreamObserver<Response> responseObserver) {
        System.out.println("Received create request: " + request);
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
        System.out.println("Received read request for ID: " + request.getId());
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
        System.out.println("Received update request for ID: " + request.getId());
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
        System.out.println("Received delete request for ID: " + request.getId());
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
        System.out.println("Received readAll request");
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
    
    // 2PC Protocol Implementation
    @Override
    public void prepare(TransactionRequest request, StreamObserver<PrepareResponse> responseObserver) {
        System.out.println("Received prepare request for transaction: " + request.getTransactionId());
        String txId = request.getTransactionId();
        String operation = request.getOperation();
        
        PrepareResponse.Builder responseBuilder = PrepareResponse.newBuilder()
                .setTransactionId(txId)
                .setPrepared(true)  // Default to success, set to false on error
                .setMessage("Ready to prepare");
        
        try {
            SensorDataStored originalValue = null;
            SensorDataStored dataToStore = null;
            String key = null;
            
            switch (operation) {
                case "CREATE":
                    if (request.hasCreateData()) {
                        SensorDataRequest createData = request.getCreateData();
                        if (createData.getTemperature() == null || createData.getTemperature().trim().isEmpty()) {
                            responseBuilder.setPrepared(false).setMessage("Temperature must not be empty");
                            break;
                        }
                        // Use a predefined key for testing or generate new one
                        key = "sensor-create-" + System.currentTimeMillis();
                        dataToStore = SensorDataStored.newBuilder()
                                .setId(key)
                                .setSensorId(createData.getSensorId())
                                .setTemperature(createData.getTemperature())
                                .build();
                        if (db.containsKey(key)) {
                            responseBuilder.setPrepared(false).setMessage("Key already exists");
                            break;
                        }
                    }
                    break;
                    
                case "UPDATE":
                    if (request.hasUpdateData()) {
                        key = request.getUpdateData().getId();
                        SensorDataRequest updateData = request.getUpdateData().getUpdatedData();
                        if (!db.containsKey(key)) {
                            responseBuilder.setPrepared(false).setMessage("Entry not found");
                            break;
                        }
                        originalValue = db.get(key);
                        dataToStore = SensorDataStored.newBuilder()
                                .setId(key)
                                .setSensorId(updateData.getSensorId())
                                .setTemperature(updateData.getTemperature())
                                .build();
                    }
                    break;
                    
                case "DELETE":
                    if (request.hasDeleteData()) {
                        key = request.getDeleteData().getId();
                        if (!db.containsKey(key)) {
                            responseBuilder.setPrepared(false).setMessage("Entry not found");
                            break;
                        }
                        originalValue = db.get(key);
                    }
                    break;
                    
                default:
                    responseBuilder.setPrepared(false).setMessage("Unknown operation");
                    break;
            }
            
            if (!responseBuilder.getPrepared()) {
                // Failed, don't store transaction data
                System.out.println("Prepare failed for transaction: " + txId + " - " + responseBuilder.getMessage());
            } else {
                // Store transaction data for commit/abort
                preparedTransactions.put(txId, new TransactionData(operation, dataToStore, originalValue));
                responseBuilder.setMessage("Prepared successfully");
                System.out.println("Prepared transaction: " + txId + " for operation: " + operation);
            }
            
        } catch (Exception e) {
            responseBuilder.setPrepared(false).setMessage("Error preparing transaction: " + e.getMessage());
            System.err.println("Error preparing transaction: " + e.getMessage());
        }
        
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
    
    @Override
    public void commit(TransactionId request, StreamObserver<Response> responseObserver) {
        System.out.println("Received commit request for transaction: " + request.getTransactionId());
        String txId = request.getTransactionId();
        TransactionData txData = preparedTransactions.remove(txId);
        
        Response.Builder responseBuilder = Response.newBuilder().setId(txId);
        
        if (txData == null) {
            responseBuilder.setSuccess(false).setMessage("No prepared transaction found");
        } else {
            try {
                switch (txData.operation) {
                    case "CREATE":
                        if (txData.data != null) {
                            db.put(txData.data.getId(), txData.data);
                            insertionOrder.add(txData.data.getId());
                            responseBuilder.setSuccess(true).setMessage("Created successfully");
                        }
                        break;
                        
                    case "UPDATE":
                        if (txData.data != null) {
                            db.put(txData.data.getId(), txData.data);
                            responseBuilder.setSuccess(true).setMessage("Updated successfully");
                        }
                        break;
                        
                    case "DELETE":
                        if (txData.originalValue != null) {
                            db.remove(txData.originalValue.getId());
                            insertionOrder.remove(txData.originalValue.getId());
                            responseBuilder.setSuccess(true).setMessage("Deleted successfully");
                        }
                        break;
                        
                    default:
                        responseBuilder.setSuccess(false).setMessage("Unknown operation");
                        break;
                }
                
                System.out.println("Committed transaction: " + txId);
                
            } catch (Exception e) {
                responseBuilder.setSuccess(false).setMessage("Error committing transaction: " + e.getMessage());
                System.err.println("Error committing transaction: " + e.getMessage());
            }
        }
        
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
    
    @Override
    public void abort(TransactionId request, StreamObserver<Response> responseObserver) {
        System.out.println("Received abort request for transaction: " + request.getTransactionId());
        String txId = request.getTransactionId();
        TransactionData txData = preparedTransactions.remove(txId);
        
        Response.Builder responseBuilder = Response.newBuilder()
                .setId(txId)
                .setSuccess(true)
                .setMessage("Transaction aborted");
        
        if (txData == null) {
            responseBuilder.setMessage("No prepared transaction found (already aborted)");
        }
        
        System.out.println("Aborted transaction: " + txId);
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}
