package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an HTTP request with its method, headers, and body.
 */
public class HttpRequest {
    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final String body;
    
    public HttpRequest(String method, String path, Map<String, String> headers, String body) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
    }
    
    public String getMethod() {
        return method;
    }
    
    public String getPath() {
        return path;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public String getBody() {
        return body;
    }
    
    /**
     * Parses an HTTP request from the input reader
     */
    public static HttpRequest parse(BufferedReader in) throws IOException {
        String requestLine = in.readLine();
        if (requestLine == null || requestLine.isEmpty()) return null;

        String[] requestParts = requestLine.split(" ", 3);
        if (requestParts.length < 3) return null;
        
        String method = requestParts[0];
        String path = requestParts[1];
        Map<String, String> headers = readHeaders(in);
        String body = null;
        
        if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT")) {
            int contentLength = Integer.parseInt(headers.getOrDefault("content-length", "0"));
            body = readBody(in, contentLength);
        }
        
        return new HttpRequest(method, path, headers, body);
    }
    
    /**
     * Reads HTTP headers from the input
     */
    private static Map<String, String> readHeaders(BufferedReader in) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String line;
        while (!(line = in.readLine()).isEmpty()) {
            String[] headerParts = line.split(":", 2);
            if (headerParts.length == 2) {
                headers.put(headerParts[0].trim().toLowerCase(), headerParts[1].trim());
            }
        }
        return headers;
    }
    
    /**
     * Reads the request body of specified length
     */
    private static String readBody(BufferedReader in, int contentLength) throws IOException {
        char[] bodyChars = new char[contentLength];
        in.read(bodyChars);
        String body = new String(bodyChars);
        System.out.println("Received JSON: " + body);
        return body;
    }
}