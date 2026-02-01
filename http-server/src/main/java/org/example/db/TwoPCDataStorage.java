package org.example.db;

import org.example.SensorData;

/**
 * Interface for DataStorage implementations that support 2-Phase Commit Protocol
 */
public interface TwoPCDataStorage extends DataStorage {
    
    /**
     * Phase 1: Prepare for a transaction
     * @param transactionId unique identifier for the transaction
     * @param operation the operation to prepare (CREATE, UPDATE, DELETE)
     * @param data the data associated with the operation
     * @return true if prepared successfully, false otherwise
     */
    boolean prepare(String transactionId, String operation, SensorData data);
    
    /**
     * Phase 2: Commit the prepared transaction
     * @param transactionId unique identifier for the transaction
     * @return true if committed successfully, false otherwise
     */
    boolean commit(String transactionId);
    
    /**
     * Abort the prepared transaction
     * @param transactionId unique identifier for the transaction
     * @return true if aborted successfully, false otherwise
     */
    boolean abort(String transactionId);
}
