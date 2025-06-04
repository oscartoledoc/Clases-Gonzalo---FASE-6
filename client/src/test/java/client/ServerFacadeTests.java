package client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.*;
import server.Server;
import ui.ServerFacade;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade serverFacade;
    private static String serverUrl;
    private static int port;
    private static Gson gson;

    @BeforeAll
    public static void init() {
        server = new Server();
        try {
            port = server.run(0);
            serverUrl = "http://localhost:" + port;
            System.out.println("Started test HTTP server on " + serverUrl);
            serverFacade = new ServerFacade(serverUrl);
            gson = new Gson();
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            fail("Server initialization failed. Unable to assign a dynamic port.");
        }
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop();
            System.out.println("Stopped test HTTP server on " + serverUrl);
        }
    }


    @BeforeEach
    public void setUp() {
        if (serverFacade != null) {
            serverFacade.setAuthToken(null);
        }
    }


    @AfterEach
    public void tearDown() {
        if (serverFacade != null) {
            try {
                serverFacade.clearServerState();
            } catch (Exception e) {
                System.err.println("Failed to clear server state after test: " + e.getMessage());
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test Register Success")
    public void testRegisterSuccess() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade is not initialized");
        String response = serverFacade.register("testUser", "password123", "test@example.com");
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        assertTrue(jsonResponse.has("authToken"), "The response must contain an authentication token");
        assertNotNull(serverFacade.getAuthToken(), "An authentication token must be generated");
        System.out.println("Register response: " + response);
    }

    @Test
    @Order(2)
    @DisplayName("Test Register Failure with Existing User")
    public void testRegisterFailure() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade is not initialized");
        serverFacade.register("testUser", "password123", "test@example.com");
        assertThrows(IOException.class, () -> serverFacade.register("testUser", "password123", "test2@example.com"),
                "An exception must be thrown when registering a duplicate user");
    }

    @Test
    @Order(3)
    @DisplayName("Test Login Success")
    public void testLoginSuccess() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade is not initialized");
        serverFacade.register("testUser", "password123", "test@example.com");
        String response = serverFacade.login("testUser", "password123");
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        assertTrue(jsonResponse.has("authToken"), "The response must contain an authentication token");
        assertNotNull(serverFacade.getAuthToken(), "An authentication token must be generated");
        System.out.println("Login response: " + response);
    }

    @Test
    @Order(4)
    @DisplayName("Test Login Failure with Wrong Password")
    public void testLoginFailure() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade is not initialized");
        serverFacade.register("testUser", "password123", "test@example.com");
        assertThrows(IOException.class, () -> serverFacade.login("testUser", "wrongPassword"),
                "An exception must be thrown for incorrect password");
    }

    @Test
    @Order(5)
    @DisplayName("Test Logout Success")
    public void testLogoutSuccess() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade is not initialized");
        serverFacade.register("testUser", "password123", "test@example.com");
        serverFacade.login("testUser", "password123");
        String response = serverFacade.logout();
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        assertTrue(jsonResponse.has("message") && jsonResponse.get("message").getAsString().contains("logged out"),
                "The response must indicate success");
        assertNull(serverFacade.getAuthToken(), "The token must be cleared after logout");
        System.out.println("Logout response: " + response);
    }

    @Test
    @Order(6)
    @DisplayName("Test Logout Failure without Auth")
    public void testLogoutFailure() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade is not initialized");
        serverFacade.setAuthToken(null);
        assertThrows(IOException.class, () -> serverFacade.logout(),
                "An exception must be thrown when attempting logout without authentication");
    }

    @Test
    @Order(7)
    @DisplayName("Test Create Game Success")
    public void testCreateGameSuccess() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade is not initialized");
        serverFacade.register("testUser", "password123", "test@example.com");
        serverFacade.login("testUser", "password123");
        String response = serverFacade.createGame("TestGame");
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        assertTrue(jsonResponse.has("gameID"), "The response must contain the game ID");
        System.out.println("Create game response: " + response);
    }

    @Test
    @Order(8)
    @DisplayName("Test Create Game Failure without Auth")
    public void testCreateGameFailure() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade is not initialized");
        serverFacade.setAuthToken(null);
        assertThrows(IOException.class, () -> serverFacade.createGame("TestGame"),
                "An exception must be thrown when creating games without authentication");
    }

    @Test
    @Order(9)
    @DisplayName("Test Join Game Success")
    public void testJoinGameSuccess() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade is not initialized");
        String registerResponse = serverFacade.register("testUser", "password123", "test@example.com");
        System.out.println("Register response: " + registerResponse);
        String loginResponse;
        try {
            loginResponse = serverFacade.login("testUser", "password123");
            System.out.println("Login response: " + loginResponse);
        } catch (IOException e) {
            System.err.println("Login failed: " + e.getMessage());
            fail("Login should succeed but threw an exception: " + e.getMessage());
        }
        String createGameResponse = null;
        try {
            createGameResponse = serverFacade.createGame("TestGame");
            System.out.println("Create game response: " + createGameResponse);
        } catch (IOException e) {
            System.err.println("Create game failed: " + e.getMessage());
            fail("Create game should succeed but threw an exception: " + e.getMessage());
        }
        String gameID = extractGameId(createGameResponse);
        String joinResponse = null;
        try {
            joinResponse = serverFacade.joinGame(gameID, "white");
            System.out.println("Join game response: " + joinResponse);
        } catch (IOException e) {
            System.err.println("Join game failed: " + e.getMessage());
            fail("Join game should succeed but threw an exception: " + e.getMessage());
        }
        JsonObject jsonResponse = gson.fromJson(joinResponse, JsonObject.class);
        assertNotNull(joinResponse, "Join game response must not be null");
    }

    @Test
    @Order(10)
    @DisplayName("Test Join Game Failure with Invalid Game")
    public void testJoinGameFailure() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade is not initialized");
        serverFacade.register("testUser", "password123", "test@example.com");
        serverFacade.login("testUser", "password123");
        assertThrows(IOException.class, () -> serverFacade.joinGame("999", "WHITE"),
                "An exception must be thrown when joining an invalid game (gameID no existente)");
    }

    @Test
    @Order(11)
    @DisplayName("Test List Games Success")
    public void testListGamesSuccess() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade is not initialized");
        serverFacade.register("testUser", "password123", "test@example.com");
        serverFacade.login("testUser", "password123");
        serverFacade.createGame("TestGame");
        String response = serverFacade.listGames();
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        assertTrue(jsonResponse.has("games"), "The response must contain a list of games");
        System.out.println("List games response: " + response);
    }

    @Test
    @Order(12)
    @DisplayName("Test List Games Failure without Auth")
    public void testListGamesFailure() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade is not initialized");
        serverFacade.setAuthToken(null);
        assertThrows(IOException.class, () -> serverFacade.listGames(),
                "An exception must be thrown when listing games without authentication");
    }

    private String extractGameId(String response) {
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        if (!jsonResponse.has("gameID")) {
            throw new IllegalArgumentException("No gameID found in response: " + response);
        }
        return jsonResponse.get("gameID").getAsString();
    }
}