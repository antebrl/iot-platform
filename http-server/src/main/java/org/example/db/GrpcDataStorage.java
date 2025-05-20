package org.example.db;

import com.google.gson.Gson;
import org.example.SensorData;
import org.example.SensorDataRequest;
import org.example.SensorDataStored;
import org.example.SensorDataStoredList;
import org.example.CreateResponse;
import org.example.Response;
import java.util.List;
import java.util.ArrayList;

public class GrpcDataStorage implements DataStorage {
    private final GrpcDatabaseClient grpcClient;
    private final Gson gson = new Gson();

    public GrpcDataStorage(String host, int port) {
        this.grpcClient = new GrpcDatabaseClient(host, port);
    }

    @Override
    public boolean create(SensorData data) {
        SensorDataRequest request = SensorDataRequest.newBuilder()
                .setSensorId(data.getSensorId())
                .setTemperature(String.valueOf(data.getTemperature()))
                .build();
        CreateResponse response = grpcClient.create(request);
        return response.getSuccess();
    }

    @Override
    public String read(String id) {
        SensorDataStored dataStored = grpcClient.read(id);
        if (dataStored != null && !dataStored.getId().isEmpty()) {
            // Konvertiere SensorDataStored zu JSON
            return gson.toJson(dataStored);
        }
        return null;
    }

    @Override
    public boolean update(SensorData data) {
         // Für Update benötigen wir die ID des Eintrags. Da der HTTP-Server nur SensorData sendet
         // und die ID von der Datenbank generiert wird, können wir hier kein direktes Update
         // basierend auf der lokalen SensorData durchführen.
         // Eine mögliche Lösung wäre, dass der HTTP-Server die ID im Update-Request mitsendet
         // oder dass wir hier erst einen Read per sensorId implementieren, um die ID zu finden.

         // Vorerst: Rückgabe false, da Update per SensorData nicht direkt im aktuellen Schema/Implementierung passt.
         System.err.println("Update by SensorData not directly supported. Needs ID.");
         return false;
    }

    @Override
    public boolean delete(String id) {
        Response response = grpcClient.delete(id);
        return response.getSuccess();
    }

    @Override
    public String readAll() {
        SensorDataStoredList response = grpcClient.readAll();
        List<SensorDataStored> entries = response.getEntriesList();
        return gson.toJson(entries);
    }
} 