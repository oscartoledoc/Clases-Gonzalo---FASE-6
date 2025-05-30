package ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.net.*;

public class ServerFacade {
    private final String serverURL;
    private String authToken;
    public ServerFacade(String serverURL){
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

    private String sendPostRequest(String endpoint, Map<String, String> data) throws IOException {
        URL url = new URL(serverURL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        String jsonInputString = mapToJson(data);
        try (OutputStream os = conn.getOutputStream()){
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
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))){
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
        conn.setRequestProperty("Authorization", authToken);
        return handleResponse(conn);
    }

    private String sendDeleteRequest(String endpoint) throws IOException {
        URL url = new URL(serverURL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Authorization", authToken);
        return handleResponse(conn);
    }

    private String sendPutRequest(String endpoint, Map<String, String> data) throws IOException {
        URL url = new URL(serverURL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", authToken);
        conn.setDoOutput(true);
        String jsonInputString = mapToJson(data);
        try (OutputStream os = conn.getOutputStream()){
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        return handleResponse(conn);
    }

    private String handleResponse(HttpURLConnection conn) throws IOException {
        int responseCode = conn.getResponseCode();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(responseCode>=400?conn.getErrorStream():conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            if (responseCode>=400) {
                throw new IOException("Server error (HTTP " + responseCode + "): " + response.toString());
            }
            return response.toString();
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

    public String register(String username, String password, String email) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("username", username);
        data.put("password", password);
        data.put("email", email);
        String response = sendPostRequest("/user", data);
        authToken = ExtractAuthToken(response);
        return response;
    }

    public String login(String username, String password) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("username", username);
        data.put("password", password);
        String response = sendPostRequest("/session", data);
        authToken = ExtractAuthToken(response);
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
        return sendPostRequest("/game", data);
    }
    public String joinGame(String gameId, String playerColor) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("gameId", gameId);
        data.put("playerColor", playerColor);
        return sendPutRequest("/game", data);
    }

    private String ExtractAuthToken(String response) {
        int start = response.indexOf("\"authToken\":\"");
        start += "\"authToken\":\"".length();
        int end = response.indexOf("\"", start);
        return response.substring(start, end);
    }



}
