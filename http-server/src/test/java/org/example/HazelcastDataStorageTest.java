package org.example.db;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.IMap;
import org.example.SensorData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;

public class HazelcastDataStorageTest {

    private HazelcastDataStorage storage;
    private IMap<String, HazelcastJsonValue> mockMap;

    @BeforeEach
    public void setUp() {
        mockMap = mock(IMap.class);
        storage = new HazelcastDataStorage(mockMap);
    }

    @Test
    public void testCreate_WithId() {
        SensorData data = SensorData.builder()
                .id("sensor-123")
                .sensorId(123)
                .temperature(22.5)
                .build();

        boolean result = storage.create(data);

        assertTrue(result);
        verify(mockMap).put(eq("sensor-123"), any(HazelcastJsonValue.class));
    }

    @Test
    public void testCreate_WithoutId_GeneratesKey() {
        SensorData data = SensorData.builder()
                .sensorId(99)
                .temperature(20.0)
                .build();

        boolean result = storage.create(data);

        assertTrue(result);
        // Prüfe, dass put mit einem Key, der "sensor-99-" enthält, aufgerufen wurde
        verify(mockMap).put(argThat(key -> key.startsWith("sensor-99-")), any(HazelcastJsonValue.class));
    }

    @Test
    public void testRead_ReturnsJsonString() {
        String key = "sensor-123";
        String json = "{\"id\":\"sensor-123\",\"sensorId\":123,\"temperature\":22.5}";

        HazelcastJsonValue mockValue = new HazelcastJsonValue(json);
        when(mockMap.get(key)).thenReturn(mockValue);

        String result = storage.read(key);

        assertEquals(json, result);
    }

    @Test
    public void testRead_ReturnsNullWhenNotFound() {
        when(mockMap.get("nonexistent")).thenReturn(null);

        String result = storage.read("nonexistent");

        assertNull(result);
    }

    @Test
    public void testUpdate_Success() {
        SensorData data = SensorData.builder()
                .id("sensor-123")
                .sensorId(123)
                .temperature(30.0)
                .build();

        when(mockMap.containsKey("sensor-123")).thenReturn(true);

        boolean result = storage.update(data);

        assertTrue(result);
        verify(mockMap).put(eq("sensor-123"), any(HazelcastJsonValue.class));
    }

    @Test
    public void testUpdate_Failure_WhenNoKey() {
        SensorData data = SensorData.builder()
                .id("sensor-999")
                .sensorId(999)
                .temperature(30.0)
                .build();

        when(mockMap.containsKey("sensor-999")).thenReturn(false);

        boolean result = storage.update(data);

        assertFalse(result);
        verify(mockMap, never()).put(anyString(), any(HazelcastJsonValue.class));
    }

    @Test
    public void testDelete_Success() {
        when(mockMap.remove("sensor-123")).thenReturn(new HazelcastJsonValue("{}"));

        boolean result = storage.delete("sensor-123");

        assertTrue(result);
        verify(mockMap).remove("sensor-123");
    }

    @Test
    public void testDelete_Failure() {
        when(mockMap.remove("sensor-404")).thenReturn(null);

        boolean result = storage.delete("sensor-404");

        assertFalse(result);
        verify(mockMap).remove("sensor-404");
    }

    @Test
    public void testReadAll_ReturnsJsonArray() {
        HazelcastJsonValue val1 = new HazelcastJsonValue("{\"id\":\"1\",\"sensorId\":1,\"temperature\":10}");
        HazelcastJsonValue val2 = new HazelcastJsonValue("{\"id\":\"2\",\"sensorId\":2,\"temperature\":20}");

        Collection<HazelcastJsonValue> values = Arrays.asList(val1, val2);
        when(mockMap.values()).thenReturn(values);

        String allDataJson = storage.readAll();

        System.out.println("DEBUG readAll JSON: " + allDataJson);

        assertTrue(allDataJson.contains("\"sensorId\":1"));
        assertTrue(allDataJson.contains("\"sensorId\":2"));
    }


    @Test
    public void testClear_CallsMapClear() {
        storage.clear();
        verify(mockMap).clear();
    }
}
