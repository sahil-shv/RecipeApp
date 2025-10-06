package dao;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Firebase Realtime Database client using HTTP REST API.
 * Provides methods to interact with Firebase database using HttpURLConnection.
 */
public class FirebaseDB {
    private static final String BASE_URL = "https://recipeapp-1dbe8-default-rtdb.firebaseio.com/";
    private static FirebaseDB instance;
    
    private FirebaseDB() {
        // Private constructor for singleton
    }
    
    /**
     * Get singleton instance
     */
    public static FirebaseDB getInstance() {
        if (instance == null) {
            instance = new FirebaseDB();
        }
        return instance;
    }
    
    /**
     * Perform a PUT request to Firebase (create/update data)
     * @param path The path relative to the base URL (e.g., "recipes/123")
     * @param jsonData The JSON data to send
     * @return The response from Firebase
     */
    public String put(String path, String jsonData) {
        try {
            String fullUrl = BASE_URL + path + ".json";
            URL url = new URL(fullUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set up the connection for PUT request
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            
            // Send the JSON data
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Read the response
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return readResponse(connection.getInputStream());
            } else {
                String error = readResponse(connection.getErrorStream());
                System.err.println("Firebase PUT error (" + responseCode + "): " + error);
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("Error in Firebase PUT: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Perform a POST request to Firebase (create data with auto-generated key)
     * @param path The path relative to the base URL (e.g., "recipes")
     * @param jsonData The JSON data to send
     * @return The response from Firebase containing the new key
     */
    public String post(String path, String jsonData) {
        try {
            String fullUrl = BASE_URL + path + ".json";
            URL url = new URL(fullUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set up the connection for POST request
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            
            // Send the JSON data
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Read the response
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return readResponse(connection.getInputStream());
            } else {
                String error = readResponse(connection.getErrorStream());
                System.err.println("Firebase POST error (" + responseCode + "): " + error);
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("Error in Firebase POST: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Perform a GET request to Firebase (read data)
     * @param path The path relative to the base URL (e.g., "recipes/123")
     * @return The JSON response from Firebase
     */
    public String get(String path) {
        try {
            String fullUrl = BASE_URL + path + ".json";
            URL url = new URL(fullUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set up the connection for GET request
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            
            // Read the response
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return readResponse(connection.getInputStream());
            } else {
                String error = readResponse(connection.getErrorStream());
                System.err.println("Firebase GET error (" + responseCode + "): " + error);
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("Error in Firebase GET: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Perform a DELETE request to Firebase (delete data)
     * @param path The path relative to the base URL (e.g., "recipes/123")
     * @return The response from Firebase
     */
    public String delete(String path) {
        try {
            String fullUrl = BASE_URL + path + ".json";
            URL url = new URL(fullUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set up the connection for DELETE request
            connection.setRequestMethod("DELETE");
            
            // Read the response
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return readResponse(connection.getInputStream());
            } else {
                String error = readResponse(connection.getErrorStream());
                System.err.println("Firebase DELETE error (" + responseCode + "): " + error);
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("Error in Firebase DELETE: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Perform an async GET request to Firebase
     * @param path The path relative to the base URL
     * @return CompletableFuture containing the JSON response
     */
    public CompletableFuture<String> getAsync(String path) {
        return CompletableFuture.supplyAsync(() -> get(path));
    }
    
    /**
     * Perform an async PUT request to Firebase
     * @param path The path relative to the base URL
     * @param jsonData The JSON data to send
     * @return CompletableFuture containing the response
     */
    public CompletableFuture<String> putAsync(String path, String jsonData) {
        return CompletableFuture.supplyAsync(() -> put(path, jsonData));
    }
    
    /**
     * Check if Firebase is reachable
     * @return true if Firebase is accessible, false otherwise
     */
    public boolean isConnected() {
        try {
            String response = get("test");
            return response != null; // Firebase returns "null" for non-existent paths, which is still a valid response
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Helper method to read response from InputStream
     */
    private String readResponse(InputStream inputStream) {
        if (inputStream == null) return null;
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (IOException e) {
            System.err.println("Error reading response: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Escape special characters for JSON
     */
    public static String escapeJson(String input) {
        if (input == null) return "null";
        
        return "\"" + input.replace("\\", "\\\\")
                          .replace("\"", "\\\"")
                          .replace("\b", "\\b")
                          .replace("\f", "\\f")
                          .replace("\n", "\\n")
                          .replace("\r", "\\r")
                          .replace("\t", "\\t") + "\"";
    }
    
    /**
     * Convert Java object to simple JSON (basic implementation)
     * For more complex objects, consider using a proper JSON library
     */
    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return escapeJson((String) obj);
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        
        // For complex objects, this is a basic implementation
        // In a real application, you'd want to use a proper JSON library like Gson or Jackson
        return obj.toString();
    }
}

