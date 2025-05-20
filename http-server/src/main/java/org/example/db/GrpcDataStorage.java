package org.example.db;

import com.google.gson.Gson;
import org.example.SensorDataProto;
import org.example.SensorDataWithId;
import org.example.SensorDataWithIdList;
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
    public boolean create(org.example.SensorData data) {
        SensorDataProto protoData = SensorDataProto.newBuilder()
                .setValue(String.valueOf(data.getTemperature()))
                .build();
        CreateResponse response = grpcClient.create(protoData);
        return response.getSuccess();
    }

    @Override
    public String read(String id) {
        SensorDataWithId dataWithId = grpcClient.read(id);
        if (dataWithId != null && dataWithId.hasData()) {
            // Konvertiere SensorDataWithId zurück zu einer Struktur, die gson verarbeiten kann
            // oder gib direkt das JSON der einzelnen Felder zurück, falls einfacher
            // Vorerst: Konvertierung zu einer Map oder einer neuen einfachen Klasse für JSON
            // Hier konvertieren wir es zurück in eine ähnliche Struktur wie die alte SensorData für JSON
            // Dies kann je nach benötigtem JSON-Format angepasst werden
            try {
                double temperature = Double.parseDouble(dataWithId.getData().getValue());
                 // Annahme: Wir brauchen die id und temperature im JSON
                return String.format("{\"id\":\"%s\", \"temperature\":%f}", dataWithId.getId(), temperature);
            } catch (NumberFormatException e) {
                System.err.println("Error parsing temperature from gRPC data: " + e.getMessage());
                return null; // Oder Fehlerbehandlung
            }
        }
        return null;
    }

    @Override
    public boolean update(org.example.SensorData data) {
        // Finde die ID des zu aktualisierenden Eintrags basierend auf der sensorId
        // Dies erfordert möglicherweise einen ReadAll oder einen anderen Mechanismus, um die ID zu finden
        // Da das aktuelle Design keine einfache Zuordnung von sensorId zu DB-ID hat,
        // ist Update per SensorData nicht direkt möglich mit dem aktuellen gRPC Read(Key) und Update(SensorDataWithId)
        // Für ein echtes Update müsste die Logik hier angepasst werden, z.B. erst Read per ID, dann Update mit neuer DataProto
        System.err.println("Update by SensorData not directly supported with current gRPC schema. Needs ID.");
        return false;
    }

    @Override
    public boolean delete(String id) {
        Response response = grpcClient.delete(id);
        return response.getSuccess();
    }

    @Override
    public String readAll() {
        SensorDataWithIdList response = grpcClient.readAll();
        List<String> dataListForJson = new ArrayList<>();
        for (SensorDataWithId entryWithId : response.getEntriesList()) {
            try {
                // Konvertiere SensorDataWithId zu einer Map oder einfachen Klasse für JSON
                 double temperature = Double.parseDouble(entryWithId.getData().getValue());
                 // Einfacher: JSON String direkt formatieren, wenn Struktur fest ist
                 dataListForJson.add(String.format("{\"id\":\"%s\", \"temperature\":%f}", entryWithId.getId(), temperature));

            } catch (NumberFormatException e) {
                System.err.println("Error parsing temperature from gRPC data in readAll: " + e.getMessage());
                // Fehlerhaften Eintrag überspringen oder loggen
            }
        }
         // Da wir Strings in der Liste haben, können wir sie direkt zu einem JSON-Array zusammensetzen
        return "[" + String.join(",", dataListForJson) + "]";
    }
} 