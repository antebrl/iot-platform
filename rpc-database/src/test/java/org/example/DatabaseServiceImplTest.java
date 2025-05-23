package org.example;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DatabaseServiceImplTest {

    private DatabaseServiceImpl service;
    private StreamObserver<CreateResponse> createObserver;
    private StreamObserver<SensorDataStored> readObserver;

    @BeforeEach
    public void setup() {
        service = new DatabaseServiceImpl();
        createObserver = mock(StreamObserver.class);
        readObserver = mock(StreamObserver.class);
    }

    @Test
    public void testCreate() {
        SensorDataRequest request = SensorDataRequest.newBuilder()
                .setSensorId(123)
                .setTemperature("25.5")
                .build();

        ArgumentCaptor<CreateResponse> captor = ArgumentCaptor.forClass(CreateResponse.class);

        service.create(request, createObserver);

        verify(createObserver).onNext(captor.capture());
        verify(createObserver).onCompleted();

        CreateResponse response = captor.getValue();
        assertTrue(response.getSuccess());
        assertNotNull(response.getId());
        assertTrue(response.getMessage().contains("Entry created"));
    }

    @Test
    public void testRead_existingId() {
        SensorDataRequest request = SensorDataRequest.newBuilder()
                .setSensorId(456)
                .setTemperature("30.0")
                .build();

        StreamObserver<CreateResponse> dummyObserver = mock(StreamObserver.class);
        ArgumentCaptor<CreateResponse> createCaptor = ArgumentCaptor.forClass(CreateResponse.class);

        service.create(request, dummyObserver);
        verify(dummyObserver).onNext(createCaptor.capture());

        String id = createCaptor.getValue().getId();

        Key key = Key.newBuilder().setId(id).build();
        ArgumentCaptor<SensorDataStored> readCaptor = ArgumentCaptor.forClass(SensorDataStored.class);

        service.read(key, readObserver);

        verify(readObserver).onNext(readCaptor.capture());
        verify(readObserver).onCompleted();

        SensorDataStored result = readCaptor.getValue();
        assertEquals(456, result.getSensorId());
        assertEquals("30.0", result.getTemperature());
        assertEquals(id, result.getId());
    }

    @Test
    public void testUpdate_existingEntry() {
        SensorDataRequest original = SensorDataRequest.newBuilder()
                .setSensorId(1)
                .setTemperature("20.0")
                .build();
        StreamObserver<CreateResponse> dummyCreateObserver = mock(StreamObserver.class);
        ArgumentCaptor<CreateResponse> createCaptor = ArgumentCaptor.forClass(CreateResponse.class);

        service.create(original, dummyCreateObserver);
        verify(dummyCreateObserver).onNext(createCaptor.capture());
        String id = createCaptor.getValue().getId();

        SensorDataRequest updatedData = SensorDataRequest.newBuilder()
                .setSensorId(2)
                .setTemperature("40.0")
                .build();
        UpdateRequest updateRequest = UpdateRequest.newBuilder()
                .setId(id)
                .setUpdatedData(updatedData)
                .build();

        StreamObserver<CreateResponse> updateObserver = mock(StreamObserver.class);
        ArgumentCaptor<CreateResponse> updateCaptor = ArgumentCaptor.forClass(CreateResponse.class);

        service.update(updateRequest, updateObserver);

        verify(updateObserver).onNext(updateCaptor.capture());
        verify(updateObserver).onCompleted();

        CreateResponse response = updateCaptor.getValue();
        assertTrue(response.getSuccess());
        assertEquals("Entry updated with ID: " + id, response.getMessage());
    }

    @Test
    public void testUpdate_nonExistingEntry() {
        UpdateRequest request = UpdateRequest.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setUpdatedData(SensorDataRequest.newBuilder()
                        .setSensorId(999)
                        .setTemperature("10.0")
                        .build())
                .build();

        StreamObserver<CreateResponse> updateObserver = mock(StreamObserver.class);
        ArgumentCaptor<CreateResponse> captor = ArgumentCaptor.forClass(CreateResponse.class);

        service.update(request, updateObserver);

        verify(updateObserver).onNext(captor.capture());
        verify(updateObserver).onCompleted();

        CreateResponse response = captor.getValue();
        assertFalse(response.getSuccess());
        assertTrue(response.getMessage().contains("not found"));
    }

    @Test
    public void testRead_nonExistingId() {
        String fakeId = UUID.randomUUID().toString();
        Key key = Key.newBuilder().setId(fakeId).build();
        ArgumentCaptor<SensorDataStored> readCaptor = ArgumentCaptor.forClass(SensorDataStored.class);

        service.read(key, readObserver);

        verify(readObserver).onNext(readCaptor.capture());
        verify(readObserver).onCompleted();

        SensorDataStored result = readCaptor.getValue();
        assertTrue(result.getId().isEmpty());
    }

    @Test
    public void testCreate_withEmptyTemperature_shouldFail() {
        SensorDataRequest request = SensorDataRequest.newBuilder()
                .setSensorId(123)
                .setTemperature("")  // Ungültig
                .build();

        ArgumentCaptor<CreateResponse> captor = ArgumentCaptor.forClass(CreateResponse.class);

        service.create(request, createObserver);

        verify(createObserver).onNext(captor.capture());
        verify(createObserver).onCompleted();

        CreateResponse response = captor.getValue();
        assertFalse(response.getSuccess());
        assertEquals("Temperature must not be empty.", response.getMessage());
        assertEquals("", response.getId());
    }
}
