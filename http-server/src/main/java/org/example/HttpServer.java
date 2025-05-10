package org.example;

import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * A simple HTTP server that handles GET and POST requests for sensor data.
 * The server stores sensor data in memory and can return it when requested.
 */
public class HttpServer {
    private static final int DEFAULT_PORT = 8080;
    
    private final int port;
    private final DataStorage dataStorage;
    private ServerSocket serverSocket;
    private boolean running = false;
    private Thread serverThread;

    public HttpServer() {
        this(DEFAULT_PORT, new DataStorage());
    }
    
    public HttpServer(int port, DataStorage dataStorage) {
        this.port = port;
        this.dataStorage = dataStorage;
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
    }
    
    /**
     * Processes the parsed request and returns an appropriate response
     */
    private HttpResponse processRequest(HttpRequest request) {
        if (request.getMethod().equalsIgnoreCase("POST")) {
            System.out.println("POST request received.");
            return handlePostRequest(request.getBody());
        } else if (request.getMethod().equalsIgnoreCase("GET")) {
            System.out.println("GET request received.");
            return handleGetRequest();
        } else {
            return handleUnsupportedMethod();
        }
    }
    
    /**
     * Handles POST requests by processing the JSON body
     */
    private HttpResponse handlePostRequest(String jsonBody) {
        try {
            dataStorage.addFromJson(jsonBody);
            return new HttpResponse.Builder()
                    .status(200, "OK")
                    .header("Content-Type", "text/plain")
                    .body("Data received.")
                    .build();
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
        String responseBody = dataStorage.asJson();
        return new HttpResponse.Builder()
                .status(200, "OK")
                .header("Content-Type", "application/json")
                .body(responseBody)
                .build();
    }
    
    /**
     * Handles unsupported HTTP methods
     */
    private HttpResponse handleUnsupportedMethod() {
        return new HttpResponse.Builder()
                .status(405, "Method Not Allowed")
                .header("Allow", "GET, POST")
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
