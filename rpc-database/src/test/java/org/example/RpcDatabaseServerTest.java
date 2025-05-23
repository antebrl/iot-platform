package org.example;

import io.grpc.Server;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class RpcDatabaseServerTest {

    private Server server;
    private ManagedChannel channel;
    private DatabaseServiceGrpc.DatabaseServiceBlockingStub blockingStub;

    @BeforeEach
    public void setUp() throws IOException {
        String serverName = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(serverName)
                .addService(new DatabaseServiceImpl())
                .directExecutor()
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();
                
        blockingStub = DatabaseServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    public void tearDown() {
        if (channel != null) {
            channel.shutdownNow();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    public void testServerStartsSuccessfully() {
        assertNotNull(server, "Server should have been initialized.");
    }

    @Test
    public void testCreateAndReadData() {
        // Create data
        SensorDataRequest createRequest = SensorDataRequest.newBuilder()
                .setSensorId(1)
                .setTemperature("25.5")
                .build();
        CreateResponse createResponse = blockingStub.create(createRequest);
        
        assertTrue(createResponse.getSuccess());
        assertFalse(createResponse.getId().isEmpty());
        
        // Read data
        Key readKey = Key.newBuilder().setId(createResponse.getId()).build();
        SensorDataStored readResponse = blockingStub.read(readKey);
        
        assertEquals(createRequest.getSensorId(), readResponse.getSensorId());
        assertEquals(createRequest.getTemperature(), readResponse.getTemperature());
    }

    @Test
    public void testUpdateData() {
        // First create data
        SensorDataRequest createRequest = SensorDataRequest.newBuilder()
                .setSensorId(1)
                .setTemperature("25.5")
                .build();
        CreateResponse createResponse = blockingStub.create(createRequest);
        
        // Update data
        SensorDataRequest updatedData = SensorDataRequest.newBuilder()
                .setSensorId(1)
                .setTemperature("30.0")
                .build();
        
        UpdateRequest updateRequest = UpdateRequest.newBuilder()
                .setId(createResponse.getId())
                .setUpdatedData(updatedData)
                .build();
                
        CreateResponse updateResponse = blockingStub.update(updateRequest);
        assertTrue(updateResponse.getSuccess());
        
        // Verify update
        Key readKey = Key.newBuilder().setId(createResponse.getId()).build();
        SensorDataStored readResponse = blockingStub.read(readKey);
        assertEquals("30.0", readResponse.getTemperature());
    }

    @Test
    public void testDeleteData() {
        // First create data
        SensorDataRequest createRequest = SensorDataRequest.newBuilder()
                .setSensorId(1)
                .setTemperature("25.5")
                .build();
        CreateResponse createResponse = blockingStub.create(createRequest);
        
        // Delete data
        DeleteRequest deleteRequest = DeleteRequest.newBuilder()
                .setId(createResponse.getId())
                .build();
        CreateResponse deleteResponse = blockingStub.delete(deleteRequest);
        assertTrue(deleteResponse.getSuccess());
        
        // Verify deletion
        Key readKey = Key.newBuilder().setId(createResponse.getId()).build();
        SensorDataStored readResponse = blockingStub.read(readKey);
        assertTrue(readResponse.getId().isEmpty());
    }

    @Test
    public void testReadAll() {
        // Create multiple entries
        SensorDataRequest request1 = SensorDataRequest.newBuilder()
                .setSensorId(1)
                .setTemperature("25.5")
                .build();
        SensorDataRequest request2 = SensorDataRequest.newBuilder()
                .setSensorId(2)
                .setTemperature("30.0")
                .build();
                
        blockingStub.create(request1);
        blockingStub.create(request2);
        
        // Read all entries
        Empty emptyRequest = Empty.newBuilder().build();
        SensorDataStoredList response = blockingStub.readAll(emptyRequest);
        
        assertFalse(response.getEntriesList().isEmpty());
        assertTrue(response.getEntriesList().size() >= 2);
    }
}
