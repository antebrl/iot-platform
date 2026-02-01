package org.example.db;

import org.example.SensorData;
import java.util.*;
import java.util.concurrent.*;

/**
 * 2-Phase Commit Coordinator that manages transactions across multiple data storage participants
 */
public class TwoPCCoordinator implements DataStorage {
    
    private final List<TwoPCDataStorage> participants;
    private final BlockingQueue<TransactionRequest> requestQueue;
    private final ExecutorService executor;
    private final Map<String, TransactionRequest> pendingTransactions;
    private volatile boolean running = true;
    
    public static class TransactionRequest {
        private final String transactionId;
        private final String operation;
        private final SensorData data;
        private final CompletableFuture<Boolean> future;
        
        public TransactionRequest(String operation, SensorData data) {
            this.transactionId = UUID.randomUUID().toString();
            this.operation = operation;
            this.data = data;
            this.future = new CompletableFuture<>();
        }
        
        // Getters
        public String getTransactionId() { return transactionId; }
        public String getOperation() { return operation; }
        public SensorData getData() { return data; }
        public CompletableFuture<Boolean> getFuture() { return future; }
    }
    
    public TwoPCCoordinator(List<TwoPCDataStorage> participants) {
        this.participants = new ArrayList<>(participants);
        this.requestQueue = new LinkedBlockingQueue<>();
        this.pendingTransactions = new ConcurrentHashMap<>();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "2PC-Coordinator");
            t.setDaemon(true);
            return t;
        });
        
        // Start the transaction processor
        executor.submit(this::processTransactions);
    }
    
    @Override
    public boolean create(SensorData data) {
        return executeTransaction("CREATE", data);
    }
    
    @Override
    public boolean update(SensorData data) {
        return executeTransaction("UPDATE", data);
    }
    
    @Override
    public boolean delete(String id) {
        SensorData deleteData = SensorData.builder().id(id).build();
        return executeTransaction("DELETE", deleteData);
    }
    
    @Override
    public String read(String id) {
        // Read operations don't need 2PC, use the first participant
        if (!participants.isEmpty()) {
            return participants.get(0).read(id);
        }
        return null;
    }
    
    @Override
    public String readAll() {
        // Read operations don't need 2PC, use the first participant
        if (!participants.isEmpty()) {
            return participants.get(0).readAll();
        }
        return "[]";
    }
    
    @Override
    public void clear() {
        for (TwoPCDataStorage participant : participants) {
            participant.clear();
        }
    }
    
    private boolean executeTransaction(String operation, SensorData data) {
        TransactionRequest request = new TransactionRequest(operation, data);
        
        try {
            requestQueue.offer(request, 10, TimeUnit.SECONDS);
            return request.getFuture().get(30, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.err.println("Transaction failed: " + e.getMessage());
            return false;
        }
    }
    
    private void processTransactions() {
        while (running) {
            try {
                TransactionRequest request = requestQueue.take();
                boolean success = execute2PC(request);
                request.getFuture().complete(success);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error processing transaction: " + e.getMessage());
            }
        }
    }
    
    private boolean execute2PC(TransactionRequest request) {
        String txId = request.getTransactionId();
        pendingTransactions.put(txId, request);
        
        try {
            // Phase 1: Prepare
            System.out.println("2PC Phase 1 - Prepare for transaction: " + txId);
            List<TwoPCDataStorage> preparedParticipants = new ArrayList<>();
            
            for (TwoPCDataStorage participant : participants) {
                boolean prepared = participant.prepare(txId, request.getOperation(), request.getData());
                if (prepared) {
                    preparedParticipants.add(participant);
                } else {
                    System.err.println("Participant failed to prepare for transaction: " + txId);
                    // Abort all prepared participants
                    abortParticipants(txId, preparedParticipants);
                    return false;
                }
            }
            
            // Phase 2: Commit
            System.out.println("2PC Phase 2 - Commit transaction: " + txId);
            boolean allCommitted = true;
            
            for (TwoPCDataStorage participant : preparedParticipants) {
                boolean committed = participant.commit(txId);
                if (!committed) {
                    System.err.println("Participant failed to commit transaction: " + txId);
                    allCommitted = false;
                }
            }
            
            if (!allCommitted) {
                // This is a critical situation - some participants committed, others didn't
                System.err.println("CRITICAL: Inconsistent state for transaction: " + txId);
            }
            
            return allCommitted;
            
        } finally {
            pendingTransactions.remove(txId);
        }
    }
    
    private void abortParticipants(String txId, List<TwoPCDataStorage> participants) {
        System.out.println("Aborting transaction: " + txId);
        for (TwoPCDataStorage participant : participants) {
            try {
                participant.abort(txId);
            } catch (Exception e) {
                System.err.println("Error aborting participant for transaction " + txId + ": " + e.getMessage());
            }
        }
    }
    
    public void shutdown() {
        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
