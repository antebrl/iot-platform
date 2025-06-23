package org.example;

import com.google.gson.JsonSyntaxException;
import com.google.gson.Gson;

import java.io.*;
import java.net.*;
import java.util.*;

import org.example.db.DataStorage;
import org.example.db.InMemoryDataStorage;
import org.example.db.GrpcDataStorage;
import org.example.SensorData;

/**
 * A simple HTTP server that handles GET and POST requests for sensor data.
 * The server stores sensor data in memory and can return it when requested.
 */
public class HttpServer {
    private static final int DEFAULT_PORT = 8080;
    
    private final int port;
    private final DataStorage dataStorage;
    private final DataStorage hazelcastStorage;
    private ServerSocket serverSocket;
    private boolean running = false;
    private Thread serverThread;
    private final Gson gson = new Gson();

    public HttpServer() {
        String rpcHost = System.getenv().getOrDefault("RPC_DATABASE_HOST", "localhost");
        this.port = DEFAULT_PORT;
        this.dataStorage = new GrpcDataStorage(rpcHost, 50051);
        this.hazelcastStorage = new InMemoryDataStorage();
    }
    
    public HttpServer(int port, DataStorage dataStorage, DataStorage hazelcastStorage) {
        this.port = port;
        this.dataStorage = dataStorage;
        this.hazelcastStorage = hazelcastStorage;
    }
    
    public void start() throws IOException {
        if (running) return;
        
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("Server runs on port " + port);
        
        serverThread = new Thread(() -> {
            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    new Thread(() -> handleClient(client)).start();
                } catch (IOException e) {
                    if (running) {
                        e.printStackTrace();
                    }
                }
            }
        });
        
        serverThread.start();
    }
    
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        if (serverThread != null) {
            try {
                serverThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public int getPort() {
        return port;
    }
    
    public DataStorage getDataStorage() {
        return dataStorage;
    }
    
    /**
     * Handles a client connection by processing the HTTP request and sending a response
     */
    private void handleClient(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()))) {

            // Parse the request
            HttpRequest request = HttpRequest.parse(in);
            if (request == null) return;
            
            // Process the request and get a response
            HttpResponse response = processRequest(request);
            
            // Send the response
            out.write(response.toString());
            out.flush();

        } catch (IOException e) {
            sendErrorResponse(client, e);
            e.printStackTrace();
        }
    }    /**
     * Processes the parsed request and returns an appropriate response
     */
    private HttpResponse processRequest(HttpRequest request) {
        String method = request.getMethod().toUpperCase();
        String path = request.getPath();
        
        switch (method) {
            case "POST":
                System.out.println("POST request received.");
                // POST requests should go to root path /
                if ("/".equals(path)) {
                    return handlePostRequest(request.getBody());
                } else {
                    return new HttpResponse.Builder()
                            .status(400, "Bad Request")
                            .header("Content-Type", "text/plain")
                            .body("Invalid POST request path. Expected format: /")
                            .build();
                }
            case "GET":
                System.out.println("GET request received.");
                // GET requests can be to root path / or to /{id}
                if ("/".equals(path)) {
                    return handleGetRequest();
                } else if (path.matches("^/[^/]+$")) { // Pattern: /{id}
                    String uuid = path.substring(1); // Remove leading slash
                    return handleGetSingleRequest(uuid);
                } else {
                    return new HttpResponse.Builder()
                            .status(400, "Bad Request")
                            .header("Content-Type", "text/plain")
                            .body("Invalid GET request path. Expected format: / or /{id}")
                            .build();
                }
            case "PUT":
                System.out.println("PUT request received.");
                // PUT requests should go to /{id}
                if (path.matches("^/[^/]+$")) { // Pattern: /{id}
                    String uuid = path.substring(1); // Remove leading slash
                    return handlePutRequest(request.getBody(), uuid);
                } else {
                    return new HttpResponse.Builder()
                            .status(400, "Bad Request")
                            .header("Content-Type", "text/plain")
                            .body("Invalid PUT request path. Expected format: /{id}")
                            .build();
                }
            case "DELETE":
                System.out.println("DELETE request received.");
                // DELETE requests should go to /{id}
                if (path.matches("^/[^/]+$")) { // Pattern: /{id}
                    String uuid = path.substring(1); // Remove leading slash
                    return handleDeleteRequest(uuid);
                } else {
                    return new HttpResponse.Builder()
                            .status(400, "Bad Request")
                            .header("Content-Type", "text/plain")
                            .body("Invalid DELETE request path. Expected format: /{id}")
                            .build();
                }
            default:
                return handleUnsupportedMethod();
        }
    }
    
    /**
     * Handles POST requests by processing the JSON body
     */
    private HttpResponse handlePostRequest(String jsonBody) {
        try {
            SensorData data = gson.fromJson(jsonBody, SensorData.class);
            boolean success = dataStorage.create(data);
            if (success) {
                // Hazelcast asynchron nach HTTP-Antwort
                new Thread(() -> hazelcastStorage.create(data)).start();
                return new HttpResponse.Builder()
                        .status(200, "OK")
                        .header("Content-Type", "text/plain")
                        .body("Data received.")
                        .build();
            } else {
                return new HttpResponse.Builder()
                        .status(500, "Internal Server Error")
                        .header("Content-Type", "text/plain")
                        .body("Failed to create data.")
                        .build();
            }
        } catch (JsonSyntaxException e) {
            return new HttpResponse.Builder()
                    .status(400, "Bad Request")
                    .header("Content-Type", "text/plain")
                    .body("Invalid JSON: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * Handles GET requests by returning all data as JSON
     */
    private HttpResponse handleGetRequest() {
        String responseBody = dataStorage.readAll();
        return new HttpResponse.Builder()
                .status(200, "OK")
                .header("Content-Type", "application/json")
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET")
                .body(responseBody)
                .build();
    }
    
    /**
     * Handles GET requests for a single item by ID
     */
    private HttpResponse handleGetSingleRequest(String uuid) {
        String responseBody = dataStorage.read(uuid);
        if (responseBody != null) {
            return new HttpResponse.Builder()
                    .status(200, "OK")
                    .header("Content-Type", "application/json")
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET")
                    .body(responseBody)
                    .build();
        } else {
            return new HttpResponse.Builder()
                    .status(404, "Not Found")
                    .header("Content-Type", "text/plain")
                    .body("Data not found.")
                    .build();
        }
    }
    
    /**
     * Handles PUT requests for updating existing data
     */
    private HttpResponse handlePutRequest(String jsonBody, String uuid) {
        try {
            SensorData data = gson.fromJson(jsonBody, SensorData.class);
            
            // Set the ID from the URL path if not present in the JSON body
            if (data.getId() == null || data.getId().isEmpty()) {
                data = SensorData.builder()
                        .id(uuid)
                        .sensorId(data.getSensorId())
                        .temperature(data.getTemperature())
                        .build();
            } else if (!data.getId().equals(uuid)) {
                // ID in body doesn't match ID in URL
                return new HttpResponse.Builder()
                        .status(400, "Bad Request")
                        .header("Content-Type", "text/plain")
                        .body("ID in request body does not match ID in URL path.")
                        .build();
            }
            
            boolean success = dataStorage.update(data);
            if (success) {
                return new HttpResponse.Builder()
                        .status(200, "OK")
                        .header("Content-Type", "text/plain")
                        .body("Data updated successfully.")
                        .build();
            } else {
                return new HttpResponse.Builder()
                        .status(404, "Not Found")
                        .header("Content-Type", "text/plain")
                        .body("Data not found or update failed.")
                        .build();
            }
        } catch (JsonSyntaxException e) {
            return new HttpResponse.Builder()
                    .status(400, "Bad Request")
                    .header("Content-Type", "text/plain")
                    .body("Invalid JSON: " + e.getMessage())
                    .build();
        }
    }    /**
     * Handles DELETE requests by using the provided UUID
     */
    private HttpResponse handleDeleteRequest(String uuid) {
        boolean success = dataStorage.delete(uuid);
        if (success) {
            return new HttpResponse.Builder()
                    .status(200, "OK")
                    .header("Content-Type", "text/plain")
                    .body("Data deleted successfully.")
                    .build();
        } else {
            return new HttpResponse.Builder()
                    .status(404, "Not Found")
                    .header("Content-Type", "text/plain")
                    .body("Data not found or deletion failed.")
                    .build();
        }
    }
    
    /**
     * Handles unsupported HTTP methods
     */
    private HttpResponse handleUnsupportedMethod() {
        return new HttpResponse.Builder()
                .status(405, "Method Not Allowed")
                .header("Allow", "GET, POST, PUT, DELETE")
                .body("Method not supported.")
                .build();
    }
    
    /**
     * Creates an error response for internal server errors
     */
    private HttpResponse createErrorResponse(Exception e) {
        return new HttpResponse.Builder()
                .status(500, "Internal Server Error")
                .header("Content-Type", "text/plain")
                .body("An internal server error occurred.\n")
                .build();
    }
    
    /**
     * Sends an error response when an exception occurs
     */
    private void sendErrorResponse(Socket client, Exception e) {
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()))) {
            HttpResponse errorResponse = createErrorResponse(e);
            out.write(errorResponse.toString());
            out.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
