package org.example;

import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.net.*;
import java.util.*;

public class HttpServer {
    private static final int PORT = 8080;
    private static final DataStorage dataStorage = new DataStorage();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server läuft auf Port " + PORT);

        while (true) {
            Socket client = serverSocket.accept();
            new Thread(() -> handleClient(client)).start();
        }
    }

    private static void handleClient(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()))) {

            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            String method = requestLine.split(" ")[0];
            int contentLength = 0;
            Map<String, String> headers = new HashMap<>();

            // Read headers
            String line;
            while (!(line = in.readLine()).isEmpty()) {
                String[] headerParts = line.split(":", 2);
                if (headerParts.length == 2) {
                    headers.put(headerParts[0].trim().toLowerCase(), headerParts[1].trim());
                }
            }

            if (method.equalsIgnoreCase("POST")) {
                contentLength = Integer.parseInt(headers.getOrDefault("content-length", "0"));
                char[] bodyChars = new char[contentLength];
                in.read(bodyChars);
                String jsonBody = new String(bodyChars);

                try {
                    dataStorage.addFromJson(jsonBody);

                    HttpResponse response = new HttpResponse.Builder()
                            .status(200, "OK")
                            .header("Content-Type", "text/plain")
                            .body("Data received.")
                            .build();
                    out.write(response.toString());

                } catch (JsonSyntaxException e) {
                    HttpResponse response = new HttpResponse.Builder()
                            .status(400, "Bad Request")
                            .header("Content-Type", "text/plain")
                            .body("Invalid JSON: " + e.getMessage())
                            .build();
                    out.write(response.toString());
                }

            } else if (method.equalsIgnoreCase("GET")) {
                String responseBody = dataStorage.asText();

                HttpResponse response = new HttpResponse.Builder()
                        .status(200, "OK")
                        .header("Content-Type", "text/plain")
                        .body(responseBody)
                        .build();

                out.write(response.toString());
            } else {
                HttpResponse response = new HttpResponse.Builder()
                        .status(405, "Method Not Allowed")
                        .header("Allow", "GET, POST")
                        .body("Method not supported.")
                        .build();

                out.write(response.toString());
            }

            out.flush();

        } catch (IOException e) {
            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()))) {
                HttpResponse errorResponse = new HttpResponse.Builder()
                        .status(500, "Internal Server Error")
                        .header("Content-Type", "text/plain")
                        .body("An internal server error occurred.\n")
                        .build();

                out.write(errorResponse.toString());
                out.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            e.printStackTrace();
        }
    }
}
