package org.example;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Integration tests for the DatabaseService RPC server
 */
public class DatabaseServiceIntegrationTest {

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private DatabaseServiceGrpc.DatabaseServiceBlockingStub blockingStub;
    private DatabaseServiceImpl service;

    @Before
    public void setUp() throws Exception {
        // Generate a unique in-process server name
        String serverName = InProcessServerBuilder.generateName();

        // Create service implementation
        service = new DatabaseServiceImpl();

        // Create in-process server
        Server server = InProcessServerBuilder
                .forName(serverName)
                .directExecutor()
                .addService(service)
                .build()
                .start();

        // Register for automatic graceful shutdown
        grpcCleanup.register(server);

        // Create channel
        ManagedChannel channel = InProcessChannelBuilder
                .forName(serverName)
                .directExecutor()
                .build();

        // Register for automatic graceful shutdown
        grpcCleanup.register(channel);

        // Create blocking stub
        blockingStub = DatabaseServiceGrpc.newBlockingStub(channel);
    }

    @Test
    public void testCreateSensorData() {
        // Test creating sensor data
        SensorDataRequest request = SensorDataRequest.newBuilder()
                .setSensorId(1)
                .setTemperature("25.5")
                .build();

        CreateResponse response = blockingStub.create(request);

        assertTrue("Create should be successful", response.getSuccess());
        assertFalse("ID should not be empty", response.getId().isEmpty());
        assertEquals("Response message should indicate success", 
                "Entry created with ID: " + response.getId(), response.getMessage());
    }

    @Test
    public void testCreateWithEmptyTemperature() {
        // Test creating sensor data with empty temperature
        SensorDataRequest request = SensorDataRequest.newBuilder()
                .setSensorId(1)
                .setTemperature("")
                .build();

        CreateResponse response = blockingStub.create(request);

        assertFalse("Create should fail", response.getSuccess());
        assertTrue("ID should be empty", response.getId().isEmpty());
        assertEquals("Response message should indicate error", 
                "Temperature must not be empty.", response.getMessage());
    }

    @Test
    public void testCreateAndRead() {
        // First create a sensor data entry
        SensorDataRequest createRequest = SensorDataRequest.newBuilder()
                .setSensorId(42)
                .setTemperature("23.7")
                .build();

        CreateResponse createResponse = blockingStub.create(createRequest);
        assertTrue("Create should be successful", createResponse.getSuccess());
        String createdId = createResponse.getId();

        // Now read it back
        Key readKey = Key.newBuilder().setId(createdId).build();
        SensorDataStored readResponse = blockingStub.read(readKey);

        assertEquals("ID should match", createdId, readResponse.getId());
        assertEquals("Sensor ID should match", 42, readResponse.getSensorId());
        assertEquals("Temperature should match", "23.7", readResponse.getTemperature());
    }

    @Test
    public void testReadNonExistentData() {
        // Try to read data that doesn't exist
        Key readKey = Key.newBuilder().setId("non-existent-id").build();
        SensorDataStored readResponse = blockingStub.read(readKey);

        // Should return empty SensorDataStored
        assertTrue("ID should be empty for non-existent data", readResponse.getId().isEmpty());
        assertEquals("Sensor ID should be 0 for non-existent data", 0, readResponse.getSensorId());
        assertTrue("Temperature should be empty for non-existent data", readResponse.getTemperature().isEmpty());
    }

    @Test
    public void testUpdateExistingData() {
        // First create a sensor data entry
        SensorDataRequest createRequest = SensorDataRequest.newBuilder()
                .setSensorId(100)
                .setTemperature("20.0")
                .build();

        CreateResponse createResponse = blockingStub.create(createRequest);
        String createdId = createResponse.getId();

        // Update the entry
        SensorDataRequest updatedData = SensorDataRequest.newBuilder()
                .setSensorId(101)
                .setTemperature("30.0")
                .build();

        UpdateRequest updateRequest = UpdateRequest.newBuilder()
                .setId(createdId)
                .setUpdatedData(updatedData)
                .build();

        CreateResponse updateResponse = blockingStub.update(updateRequest);

        assertTrue("Update should be successful", updateResponse.getSuccess());
        assertEquals("Update ID should match", createdId, updateResponse.getId());
        assertEquals("Update message should indicate success", 
                "Entry updated with ID: " + createdId, updateResponse.getMessage());

        // Verify the update by reading the data
        Key readKey = Key.newBuilder().setId(createdId).build();
        SensorDataStored readResponse = blockingStub.read(readKey);

        assertEquals("Updated sensor ID should match", 101, readResponse.getSensorId());
        assertEquals("Updated temperature should match", "30.0", readResponse.getTemperature());
    }

    @Test
    public void testUpdateNonExistentData() {
        // Try to update data that doesn't exist
        SensorDataRequest updatedData = SensorDataRequest.newBuilder()
                .setSensorId(999)
                .setTemperature("99.9")
                .build();

        UpdateRequest updateRequest = UpdateRequest.newBuilder()
                .setId("non-existent-id")
                .setUpdatedData(updatedData)
                .build();

        CreateResponse updateResponse = blockingStub.update(updateRequest);

        assertFalse("Update should fail for non-existent data", updateResponse.getSuccess());
        assertEquals("Update ID should match request", "non-existent-id", updateResponse.getId());
        assertEquals("Update message should indicate failure", 
                "Entry with ID: non-existent-id not found.", updateResponse.getMessage());
    }

    @Test
    public void testDeleteExistingData() {
        // First create a sensor data entry
        SensorDataRequest createRequest = SensorDataRequest.newBuilder()
                .setSensorId(200)
                .setTemperature("18.5")
                .build();

        CreateResponse createResponse = blockingStub.create(createRequest);
        String createdId = createResponse.getId();

        // Delete the entry
        DeleteRequest deleteRequest = DeleteRequest.newBuilder()
                .setId(createdId)
                .build();

        CreateResponse deleteResponse = blockingStub.delete(deleteRequest);

        assertTrue("Delete should be successful", deleteResponse.getSuccess());
        assertEquals("Delete ID should match", createdId, deleteResponse.getId());
        assertEquals("Delete message should indicate success", 
                "Entry deleted with ID: " + createdId, deleteResponse.getMessage());

        // Verify deletion by trying to read the data
        Key readKey = Key.newBuilder().setId(createdId).build();
        SensorDataStored readResponse = blockingStub.read(readKey);

        assertTrue("Deleted data should not be found", readResponse.getId().isEmpty());
    }

    @Test
    public void testDeleteNonExistentData() {
        // Try to delete data that doesn't exist
        DeleteRequest deleteRequest = DeleteRequest.newBuilder()
                .setId("non-existent-id")
                .build();

        CreateResponse deleteResponse = blockingStub.delete(deleteRequest);

        assertFalse("Delete should fail for non-existent data", deleteResponse.getSuccess());
        assertEquals("Delete ID should match request", "non-existent-id", deleteResponse.getId());
        assertEquals("Delete message should indicate failure", 
                "Entry with ID: non-existent-id not found.", deleteResponse.getMessage());
    }

    @Test
    public void testReadAllEmptyDatabase() {
        // Read all entries from empty database
        Empty emptyRequest = Empty.newBuilder().build();
        SensorDataStoredList response = blockingStub.readAll(emptyRequest);

        assertEquals("Empty database should return empty list", 0, response.getEntriesCount());
    }

    @Test
    public void testReadAllWithMultipleEntries() {
        // Create multiple sensor data entries
        String[] temperatures = {"15.0", "20.0", "25.0"};
        int[] sensorIds = {1, 2, 3};
        String[] createdIds = new String[3];

        for (int i = 0; i < 3; i++) {
            SensorDataRequest createRequest = SensorDataRequest.newBuilder()
                    .setSensorId(sensorIds[i])
                    .setTemperature(temperatures[i])
                    .build();

            CreateResponse createResponse = blockingStub.create(createRequest);
            createdIds[i] = createResponse.getId();
            assertTrue("Create should be successful", createResponse.getSuccess());
        }

        // Read all entries
        Empty emptyRequest = Empty.newBuilder().build();
        SensorDataStoredList response = blockingStub.readAll(emptyRequest);

        assertEquals("Should return all created entries", 3, response.getEntriesCount());

        // Verify the entries are returned in insertion order
        for (int i = 0; i < 3; i++) {
            SensorDataStored entry = response.getEntries(i);
            assertEquals("ID should match", createdIds[i], entry.getId());
            assertEquals("Sensor ID should match", sensorIds[i], entry.getSensorId());
            assertEquals("Temperature should match", temperatures[i], entry.getTemperature());
        }
    }

    @Test
    public void testCompleteWorkflow() {
        // Test a complete workflow: Create -> Read -> Update -> Read -> Delete -> Read
        
        // 1. Create
        SensorDataRequest createRequest = SensorDataRequest.newBuilder()
                .setSensorId(500)
                .setTemperature("22.2")
                .build();
        CreateResponse createResponse = blockingStub.create(createRequest);
        String id = createResponse.getId();
        assertTrue("Create should be successful", createResponse.getSuccess());

        // 2. Read
        Key readKey = Key.newBuilder().setId(id).build();
        SensorDataStored readResponse1 = blockingStub.read(readKey);
        assertEquals("Temperature should match initial value", "22.2", readResponse1.getTemperature());

        // 3. Update
        UpdateRequest updateRequest = UpdateRequest.newBuilder()
                .setId(id)
                .setUpdatedData(SensorDataRequest.newBuilder()
                        .setSensorId(501)
                        .setTemperature("33.3")
                        .build())
                .build();
        CreateResponse updateResponse = blockingStub.update(updateRequest);
        assertTrue("Update should be successful", updateResponse.getSuccess());

        // 4. Read again
        SensorDataStored readResponse2 = blockingStub.read(readKey);
        assertEquals("Temperature should match updated value", "33.3", readResponse2.getTemperature());
        assertEquals("Sensor ID should match updated value", 501, readResponse2.getSensorId());

        // 5. Delete
        DeleteRequest deleteRequest = DeleteRequest.newBuilder().setId(id).build();
        CreateResponse deleteResponse = blockingStub.delete(deleteRequest);
        assertTrue("Delete should be successful", deleteResponse.getSuccess());

        // 6. Read after delete
        SensorDataStored readResponse3 = blockingStub.read(readKey);
        assertTrue("Data should not exist after deletion", readResponse3.getId().isEmpty());
    }
}
