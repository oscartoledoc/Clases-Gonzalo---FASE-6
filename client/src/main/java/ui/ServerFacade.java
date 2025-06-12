package ui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ServerFacade {
    private final String serverURL;
    private String authToken; // Este authToken se debería actualizar en login/register

    public ServerFacade(String serverURL){
        this.serverURL = serverURL;
        this.authToken = null; // Inicialmente null
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    // ... (Mantén tus otros métodos como sendPostRequest, sendGetRequest, etc.)

    // MODIFICAR ESTE MÉTODO:
    public String joinGame(String gameID, String playerColor, String authToken) throws IOException { // Agregado authToken
        Map<String, String> data = new HashMap<>();
        data.put("gameID", gameID);
        // playerColor puede ser null si es observador
        if (playerColor != null && !playerColor.isEmpty()) {
            data.put("playerColor", playerColor);
        }
        return sendPutRequestWithAuth("/game", data, authToken); // Llama a una nueva función que use authToken
    }

    // AÑADIR ESTE MÉTODO (si no existe):
    private String sendPutRequestWithAuth(String endpoint, Map<String, String> data, String token) throws IOException {
        URL url = new URL(serverURL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/json");
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", token);
        }
        conn.setDoOutput(true);
        String jsonInputString = new Gson().toJson(data);
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        return handleResponse(conn);
    }
    // ... (Mantén tus otros métodos como login, register, createGame, etc.)

    // ASEGÚRATE DE QUE TUS MÉTODOS LOGIN Y REGISTER ESTABLEZCAN EL AUTH TOKEN EN LA FACHADA
    public String login(String username, String password) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("username", username);
        data.put("password", password);
        String response = sendPostRequest("/session", data);
        if (!response.contains("Error")) {
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            this.authToken = jsonResponse.get("authToken").getAsString(); // ESTABLECE EL AUTH TOKEN AQUÍ
        }
        return response;
    }

    public String register(String username, String password, String email) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("username", username);
        data.put("password", password);
        data.put("email", email);
        String response = sendPostRequest("/user", data);
        if (!response.contains("Error")) {
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            this.authToken = jsonResponse.get("authToken").getAsString(); // ESTABLECE EL AUTH TOKEN AQUÍ
        }
        return response;
    }

    // ... (el resto de tu clase ServerFacade)

    // Agrega estos métodos sendGetRequestWithAuth y sendDeleteRequestWithAuth
    // para que usen el authToken
    public String listGames(String authToken) throws IOException {
        return sendGetRequestWithAuth("/game", authToken);
    }

    public String logout(String authToken) throws IOException {
        return sendDeleteRequestWithAuth("/session", authToken);
    }

    public String createGame(String gameName, String authToken) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("gameName", gameName);
        return sendPostRequestWithAuth("/game", data, authToken);
    }

    private String sendGetRequestWithAuth(String endpoint, String token) throws IOException {
        URL url = new URL(serverURL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", token);
        }
        return handleResponse(conn);
    }

    private String sendPostRequestWithAuth(String endpoint, Map<String, String> data, String token) throws IOException {
        URL url = new URL(serverURL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", token);
        }
        conn.setDoOutput(true);
        String jsonInputString = new Gson().toJson(data);
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        return handleResponse(conn);
    }

    private String sendDeleteRequestWithAuth(String endpoint, String token) throws IOException {
        URL url = new URL(serverURL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", token);
        }
        return handleResponse(conn);
    }


    // Método auxiliar para manejar la respuesta HTTP
    private String handleResponse(HttpURLConnection conn) throws IOException {
        int responseCode = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream(), "utf-8"))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        if (responseCode >= 200 && responseCode < 300) {
            return response.toString();
        } else {
            return "Error (" + responseCode + "): " + response.toString();
        }
    }

    // Método auxiliar para convertir Map a JSON
    private String mapToJson(Map<String, String> map) {
        return new Gson().toJson(map);
    }

    // Método para limpiar el estado del servidor (usado en pruebas)
    public void clearServerState() throws IOException {
        sendDeleteRequest("/db"); // Asume que tienes un sendDeleteRequest sin auth
    }

    private String sendDeleteRequest(String endpoint) throws IOException {
        URL url = new URL(serverURL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        // No authentication needed for /db clear
        return handleResponse(conn);
    }

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
        return handleResponse(conn);
    }
}