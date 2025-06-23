package org.example;

import org.example.db.InMemoryDataStorage;
import org.junit.jupiter.api.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the HTTP Server.
 * 
 * This test class verifies that the HTTP server functions correctly in various scenarios,
 * including handling GET/POST requests, error conditions, and concurrent access.
 * Tests use an actual server instance on a random port and make real HTTP requests.
 */
public class HttpServerIntegrationTest {

    private static HttpServer server;
    private static int PORT;
    private static final String BASE_URL = "http://localhost:";

    /**
     * Set up the test environment by starting an HTTP server on a random port.
     * Using a random port avoids conflicts when running tests in parallel or
     * on CI systems where ports might be in use.
     */
    @BeforeAll
    public static void setUp() throws IOException {
        PORT = 8080;
        
        InMemoryDataStorage dataStorage = new InMemoryDataStorage();
        server = new HttpServer(PORT, dataStorage, dataStorage);
        server.start();
        
        // Wait a bit for the server to start
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Shut down the server after all tests have completed.
     */
    @AfterAll
    public static void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Clear any stored data after each test to ensure test isolation.
     */
    @AfterEach    public void clearData() {
        // Clear any data added during tests
        server.getDataStorage().clear();
    }

    /**
     * Tests GET request when no data exists in the storage.
     * Verifies that the server returns an empty response with 200 status code.
     */    @Test
    public void testGetEmptyData() throws IOException {
        URL url = new URL(BASE_URL + PORT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);

        String response = readResponse(connection);
        // For empty data, we expect a JSON empty array
        assertEquals("[]", response.trim(), "Response should be an empty JSON array for no data");
    }

    /**
     * Tests POST request with valid JSON data.
     * Verifies that the server accepts the data and returns a 200 status code.
     */
    @Test
    public void testPostValidData() throws IOException {
        String sensorData = "{\"sensorId\": 1, \"temperature\": 23.5}";
        
        URL url = new URL(BASE_URL + PORT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = sensorData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);
        
        String response = readResponse(connection);
        assertEquals("Data received.", response.trim());
    }

    /**
     * Tests POST request with invalid JSON data.
     * Verifies that the server rejects the data and returns a 400 Bad Request status code.
     */
    @Test
    public void testPostInvalidData() throws IOException {
        String invalidData = "{\"sensorId\": 1, \"temperature\": }"; // Invalid JSON - missing value
        
        URL url = new URL(BASE_URL + PORT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = invalidData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        assertEquals(400, responseCode, "Invalid JSON should return 400 Bad Request");
        
        String response = readResponse(connection);
        assertTrue(response.contains("Invalid JSON"), "Response should contain error message");
    }

    /**
     * Tests posting multiple data items and then retrieving them.
     * Verifies that the server correctly stores and returns all the posted data.
     */    @Test
    public void testPostAndGetData() throws IOException {
        // First post some data
        String sensorData1 = "{\"sensorId\": 1, \"temperature\": 23.5}";
        String sensorData2 = "{\"sensorId\": 2, \"temperature\": 18.0}";
        
        postData(sensorData1);
        postData(sensorData2);
        
        // Then get the data and verify
        URL url = new URL(BASE_URL + PORT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);
        
        String response = readResponse(connection);
        // Check for JSON response containing the data
        assertTrue(response.contains("\"sensorId\":1"), "Response should contain first sensor data");
        assertTrue(response.contains("\"temperature\":23.5"), "Response should contain first sensor temperature");
        assertTrue(response.contains("\"sensorId\":2"), "Response should contain second sensor data");
        assertTrue(response.contains("\"temperature\":18.0"), "Response should contain second sensor temperature");
    }

    /**
     * Tests that the server correctly handles unsupported HTTP methods.
     * Verifies that a 405 Method Not Allowed status code is returned with proper Allow header.
     */
    @Test
    public void testUnsupportedMethod() throws IOException {        URL url = new URL(BASE_URL + PORT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD"); // Unsupported method that HttpURLConnection accepts

        int responseCode = connection.getResponseCode();
        assertEquals(405, responseCode, "Unsupported method should return 405 Method Not Allowed");
        
        String allowHeader = connection.getHeaderField("Allow");
        assertNotNull(allowHeader, "Allow header should be present");
        assertTrue(allowHeader.contains("GET") && allowHeader.contains("POST") && 
                   allowHeader.contains("PUT") && allowHeader.contains("DELETE"), 
                   "Allow header should mention all supported methods");
    }

    /**
     * Tests the server's ability to handle concurrent requests.
     * Creates multiple threads that simultaneously POST and GET data,
     * then verifies that all the data was correctly processed.
     */
    @Test
    public void testConcurrentRequests() throws Exception {
        // Create and start multiple threads to send requests concurrently
        Thread[] threads = new Thread[10];
        
        for (int i = 0; i < 5; i++) {
            final int id = i;
            threads[i] = new Thread(() -> {
                try {
                    String data = "{\"sensorId\": " + id + ", \"temperature\": " + (20.0 + id) + "}";
                    postData(data);
                } catch (IOException e) {
                    fail("Exception in thread: " + e.getMessage());
                }
            });
        }
        
        // Some threads will read data
        for (int i = 5; i < 10; i++) {
            threads[i] = new Thread(() -> {
                try {
                    URL url = new URL(BASE_URL + PORT);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    readResponse(connection);
                } catch (IOException e) {
                    fail("Exception in thread: " + e.getMessage());
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
          // Verify the data was stored correctly
        URL url = new URL(BASE_URL + PORT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        
        String response = readResponse(connection);
        for (int i = 0; i < 5; i++) {
            assertTrue(response.contains("\"sensorId\":" + i), 
                      "Response should contain sensor ID " + i);
        }
    }    /**
     * Tests PUT request to update existing data.
     * Verifies that the server correctly updates data and returns appropriate responses.
     */
    @Test
    public void testPutRequest() throws IOException {
        // First post some data to have something to update
        String originalData = "{\"sensorId\": 1, \"temperature\": 23.5}";
        postData(originalData);
        
        // Get all data to find the generated ID
        String allDataResponse = getAllData();
        // Extract the ID from the response (assuming it's the first item in the array)
        String extractedId = extractFirstIdFromJsonArray(allDataResponse);
        assertNotNull(extractedId, "Should have extracted an ID from the response");
        
        // Now update the data with PUT request
        String updatedData = "{\"id\":\"" + extractedId + "\",\"sensorId\": 1, \"temperature\": 25.0}";
        
        URL url = new URL(BASE_URL + PORT + "/" + extractedId);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = updatedData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode, "PUT should succeed with status 200");
        
        String response = readResponse(connection);
        assertEquals("Data updated successfully.", response.trim());
    }

    /**
     * Tests PUT request with non-existent ID.
     * Verifies that the server returns 404 when trying to update non-existent data.
     */
    @Test
    public void testPutRequestNotFound() throws IOException {
        String updateData = "{\"id\":\"non-existent-id\",\"sensorId\": 1, \"temperature\": 25.0}";
        
        URL url = new URL(BASE_URL + PORT + "/non-existent-id");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = updateData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        assertEquals(404, responseCode, "PUT to non-existent ID should return 404");
        
        String response = readResponse(connection);
        assertTrue(response.contains("not found"), "Response should indicate data not found");
    }    /**
     * Tests DELETE request to remove existing data.
     * Verifies that the server correctly deletes data and returns appropriate responses.
     */
    @Test
    public void testDeleteRequest() throws IOException {
        // First post some data to have something to delete
        String testData = "{\"sensorId\": 2, \"temperature\": 20.0}";
        postData(testData);
        
        // Get all data to find the generated ID
        String allDataResponse = getAllData();
        String extractedId = extractFirstIdFromJsonArray(allDataResponse);
        assertNotNull(extractedId, "Should have extracted an ID from the response");
        
        // Now delete the data
        URL url = new URL(BASE_URL + PORT + "/" + extractedId);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode, "DELETE should succeed with status 200");
        
        String response = readResponse(connection);
        assertEquals("Data deleted successfully.", response.trim());
    }

    /**
     * Tests DELETE request with non-existent ID.
     * Verifies that the server returns 404 when trying to delete non-existent data.
     */
    @Test
    public void testDeleteRequestNotFound() throws IOException {
        URL url = new URL(BASE_URL + PORT + "/non-existent-id");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");

        int responseCode = connection.getResponseCode();
        assertEquals(404, responseCode, "DELETE to non-existent ID should return 404");
        
        String response = readResponse(connection);
        assertTrue(response.contains("not found"), "Response should indicate data not found");
    }    /**
     * Tests GET request for a specific item by ID.
     * Verifies that the server returns the correct individual item.
     */
    @Test
    public void testGetSingleItem() throws IOException {
        // First post some data
        String testData = "{\"sensorId\": 3, \"temperature\": 22.5}";
        postData(testData);
        
        // Get all data to find the generated ID
        String allDataResponse = getAllData();
        String extractedId = extractFirstIdFromJsonArray(allDataResponse);
        assertNotNull(extractedId, "Should have extracted an ID from the response");
        
        // Now get the specific item
        URL url = new URL(BASE_URL + PORT + "/" + extractedId);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode, "GET single item should succeed with status 200");
        
        String response = readResponse(connection);
        assertTrue(response.contains(extractedId), "Response should contain the requested ID");
        assertTrue(response.contains("\"sensorId\":3"), "Response should contain the sensor data");
        assertTrue(response.contains("\"temperature\":22.5"), "Response should contain the temperature data");
    }

    /**
     * Tests GET request for a non-existent item by ID.
     * Verifies that the server returns 404 for non-existent items.
     */
    @Test
    public void testGetSingleItemNotFound() throws IOException {
        URL url = new URL(BASE_URL + PORT + "/non-existent-id");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        assertEquals(404, responseCode, "GET non-existent item should return 404");
        
        String response = readResponse(connection);
        assertTrue(response.contains("not found"), "Response should indicate data not found");
    }

    /**
     * Tests invalid path patterns for various HTTP methods.
     * Verifies that the server returns 400 Bad Request for invalid URL patterns.
     */
    @Test
    public void testInvalidPathPatterns() throws IOException {
        // Test invalid POST path (should only accept /)
        testInvalidPath("POST", "/invalid/path", 400);
        
        // Test invalid PUT path (should only accept /{id})
        testInvalidPath("PUT", "/", 400);
        testInvalidPath("PUT", "/invalid/path/structure", 400);
        
        // Test invalid DELETE path (should only accept /{id})
        testInvalidPath("DELETE", "/", 400);
        testInvalidPath("DELETE", "/invalid/path/structure", 400);
        
        // Test invalid GET path (should only accept / or /{id})
        testInvalidPath("GET", "/invalid/path/structure", 400);
    }

    /**
     * Helper method to test invalid path patterns.
     */
    private void testInvalidPath(String method, String path, int expectedStatus) throws IOException {
        URL url = new URL(BASE_URL + PORT + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        
        if ("POST".equals(method) || "PUT".equals(method)) {
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = "{\"sensorId\": 1, \"temperature\": 20.0}".getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }

        int responseCode = connection.getResponseCode();
        assertEquals(expectedStatus, responseCode, 
                    method + " to " + path + " should return " + expectedStatus);
    }

    /**
     * Helper method to read HTTP response content.
     * Handles both successful and error responses.
     *
     * @param connection The HTTP connection to read from
     * @return The response as a string
     */
    private String readResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                    connection.getResponseCode() < 400 ? 
                    connection.getInputStream() : connection.getErrorStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line).append("\n");
            }
            return response.toString();
        }
    }

    /**
     * Helper method to post JSON data to the server.
     * Sends a POST request and verifies that it succeeds.
     *
     * @param jsonData The JSON data to post
     */
    private void postData(String jsonData) throws IOException {
        URL url = new URL(BASE_URL + PORT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Just to confirm post was successful
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode, "POST should succeed with status 200");
        connection.disconnect();
    }
    
    /**
     * Helper method to get all data from the server.
     * 
     * @return The JSON response containing all data
     */
    private String getAllData() throws IOException {
        URL url = new URL(BASE_URL + PORT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode, "GET all data should succeed");
        
        return readResponse(connection);
    }
    
    /**
     * Helper method to extract the first ID from a JSON array response.
     * Simple string parsing to extract the ID field from the first object in the array.
     * 
     * @param jsonArray The JSON array response
     * @return The extracted ID or null if not found
     */
    private String extractFirstIdFromJsonArray(String jsonArray) {
        // Simple string parsing to find the first "id":"value" pattern
        String pattern = "\"id\":\"";
        int startIndex = jsonArray.indexOf(pattern);
        if (startIndex == -1) {
            return null;
        }
        
        startIndex += pattern.length();
        int endIndex = jsonArray.indexOf("\"", startIndex);
        if (endIndex == -1) {
            return null;
        }
        
        return jsonArray.substring(startIndex, endIndex);
    }
}
