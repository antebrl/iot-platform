package org.example;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import org.example.HttpResponse;

public class HttpServer {
    private static final int PORT = 8080;
    private static final List<String> storedData = new ArrayList<>();

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
            String path = requestLine.split(" ")[1];
            int contentLength = 0;
            Map<String, String> headers = new HashMap<>();

            // Header lesen
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
                String data = new String(bodyChars);
                storedData.add(data);

                HttpResponse response = new HttpResponse.Builder()
                        .status(200, "OK")
                        .header("Content-Type", "text/plain")
                        .body("Data received.")
                        .build();

                out.write(response.toString());

            } else if (method.equalsIgnoreCase("GET")) {
                String responseBody = String.join("\n", storedData);

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
