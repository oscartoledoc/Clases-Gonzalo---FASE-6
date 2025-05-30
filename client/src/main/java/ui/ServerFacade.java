package ui;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ServerFacade {
    private final String serverURL;
    private String authToken;

    public ServerFacade(String serverURL) {
        this.serverURL = serverURL;
        this.authToken = null;
    }

private String extractGameId(String response) {
        if (!response.contains("\"gameID\"")) {
            throw new IllegalArgumentException("No gameID found in response: " + response);
        }
        try {
            int start = response.indexOf("\"gameID\":\"") + 9;
            int end = response.indexOf("\"", start);
            if (start == -1 || end == -1 || start >= end) {
                throw new IllegalArgumentException("Invalid gameID format in response: " + response);
            }
            return response.substring(start, end);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to extract gameID from response: " + response, e);
        }
    }

    @Test
    @DisplayName("Test Join Game Success")
    public void testJoinGameSuccess() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade no está inicializado");
        String registerResponse = serverFacade.register("testUser", "password123", "test@example.com");
        System.out.println("Register response: " + registerResponse);
        String loginResponse;
        try {
            loginResponse = serverFacade.login("testUser", "password123");
            System.out.println("Login response: " + loginResponse);
            System.out.println("AuthToken after login: " + serverFacade.getAuthToken());
        } catch (IOException e) {
            System.err.println("Login failed: " + e.getMessage());
            fail("Login should succeed but threw an exception: " + e.getMessage());
        }
        String createGameResponse;
        try {
            createGameResponse = serverFacade.createGame("TestGame");
            System.out.println("Create game response: " + createGameResponse);
        } catch (IOException e) {
            System.err.println("Create game failed: " + e.getMessage());
            fail("Create game should succeed but threw an exception: " + e.getMessage());
        }
        String gameId = extractGameId(createGameResponse);
        String response = serverFacade.joinGame(gameId, "WHITE");
        assertNotNull(response, "La respuesta de unión a juego no debe ser nula");
        assertTrue(response.contains("Joined"), "La respuesta debe indicar unión exitosa");
        System.out.println("Join game response: " + response);
    }








    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String register(String username, String password, String email) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("username", username);
        data.put("password", password);
        data.put("email", email);
        String response = sendPostRequest("/user", data);
        System.out.println("Register Response: " + response);
        authToken = ExtractAuthToken(response); // Nombre corregido
        System.out.println("Auth token after register: " + authToken); // Debug añadido
        return response;
    }

    public String login(String username, String password) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("username", username);
        data.put("password", password);
        String response = sendPostRequest("/session", data);
<<<<<<< HEAD
        authToken = ExtractAuthToken(response);
=======
        System.out.println("Login response: " + response);
        authToken = ExtractAuthToken(response); // Nombre corregido
        System.out.println("Auth token after login: " + authToken); // Debug añadido
>>>>>>> 76fcd859700d055309638101fa27d7927e906e44
        return response;
    }

    public String logout() throws IOException {
        String response = sendDeleteRequest("/session");
        authToken = null;
        return response;
    }

    public String listGames() throws IOException {
        return sendGetRequest("/game");
    }

    public String createGame(String gameName) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("gameName", gameName);
        System.out.println("Creating game with token: " + authToken); // Debug mejorado
        return sendPostRequestWithAuth("/game", data);
    }

    public String joinGame(String gameId, String playerColor) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("gameId", gameId);
        data.put("playerColor", playerColor.toLowerCase());
        return sendPutRequest("/game", data);
    }

<<<<<<< HEAD
    private String ExtractAuthToken(String response) {
        int start = response.indexOf("\"authToken\":\"");
        start += "\"authToken\":\"".length();
        int end = response.indexOf("\"", start);
        return response.substring(start, end);
    }

=======
    private String sendPostRequest(String endpoint, Map<String, String> data) throws IOException {
        URL url = new URL(serverURL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        String jsonInputString = mapToJson(data);
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        } catch (IOException e) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;
                while ((errorLine = br.readLine()) != null) {
                    errorResponse.append(errorLine.trim());
                }
                throw new IOException("Server error: " + errorResponse.toString());
            }
        } finally {
            conn.disconnect();
        }
    }

    private String sendGetRequest(String endpoint) throws IOException {
        URL url = new URL(serverURL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (authToken != null) {
            conn.setRequestProperty("Authorization", authToken);
        }
        return handleResponse(conn);
    }
>>>>>>> 76fcd859700d055309638101fa27d7927e906e44

    private String sendDeleteRequest(String endpoint) throws IOException {
        URL url = new URL(serverURL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        if (authToken != null && !authToken.isEmpty()) {
            conn.setRequestProperty("Authorization", authToken);
        }
        return handleResponse(conn);
    }

    private String sendPutRequest(String endpoint, Map<String, String> data) throws IOException {
        URL url = new URL(serverURL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/json");
        if (authToken != null) {
            conn.setRequestProperty("Authorization", authToken);
        }
        conn.setDoOutput(true);
        String jsonInputString = mapToJson(data);
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        return handleResponse(conn);
    }

    private String sendPostRequestWithAuth(String endpoint, Map<String, String> data) throws IOException {
        URL url = new URL(serverURL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        if (authToken != null && !authToken.isEmpty()) {
            conn.setRequestProperty("Authorization", authToken);
            System.out.println("Sending POST with Authorization: " + authToken); // Debug
        } else {
            System.out.println("WARNING: No auth token available!"); // Debug
        }
        conn.setDoOutput(true);
        String jsonInputString = mapToJson(data);
        System.out.println("POST body: " + jsonInputString); // Debug
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        return handleResponse(conn);
    }

    private String handleResponse(HttpURLConnection conn) throws IOException {
        int responseCode = conn.getResponseCode();
        System.out.println("HTTP Response Code: " + responseCode); // Debug añadido
        try (BufferedReader br = new BufferedReader(new InputStreamReader(responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            String responseBody = response.toString();
            System.out.println("Response body: " + responseBody); // Debug añadido
            if (responseCode >= 400) {
                throw new IOException("Server error (HTTP " + responseCode + "): " + responseBody);
            }
            return responseBody;
        } finally {
            conn.disconnect();
        }
    }

    private String mapToJson(Map<String, String> data) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (!first) {
                json.append(", ");
            }
            json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    private String ExtractAuthToken(String response) {
        System.out.println("Extracting auth token from: " + response); // Debug
        if (response == null || !response.contains("\"authToken\":\"")) {
            throw new IllegalArgumentException("No authToken found in response: " + response);
        }
        int start = response.indexOf("\"authToken\":\"") + "\"authToken\":\"".length();
        int end = response.indexOf("\"", start);
        if (start == -1 || end == -1 || start >= end) {
            throw new IllegalArgumentException("Invalid authToken format: " + response);
        }
        String token = response.substring(start, end);
        System.out.println("Extracted auth token: " + token); // Debug
        return token;
    }

    public void clearServerState() throws IOException {
        sendDeleteRequest("/db");
    }
}