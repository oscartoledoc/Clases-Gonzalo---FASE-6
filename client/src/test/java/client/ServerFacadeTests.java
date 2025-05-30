package client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.*;
import server.Server;
import ui.ServerFacade;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Esta clase contiene pruebas unitarias para la clase ServerFacade, verificando las funcionalidades
 * de registro, login, logout, creación de juegos, unión a juegos y listado de juegos. Utiliza un
 * servidor de prueba iniciado en un puerto dinámico antes de todas las pruebas.
 */
public class ServerFacadeTests {

    private static Server server; // Servidor de prueba que se ejecuta durante las pruebas
    private static ServerFacade serverFacade; // Instancia de ServerFacade para las pruebas
    private static String serverUrl; // URL dinámica basada en el puerto asignado
    private static int port; // Puerto dinámico asignado al servidor
    private static Gson gson;

    /**
     * Método de inicialización que se ejecuta una vez antes de todas las pruebas.
     * Inicia el servidor en un puerto dinámico y configura la instancia de ServerFacade con la URL generada.
     */
    @BeforeAll
    public static void init() {
        server = new Server();
        try {
            port = server.run(0); // Inicia el servidor en un puerto dinámico
            serverUrl = "http://localhost:" + port; // Construye la URL con el puerto dinámico
            System.out.println("Started test HTTP server on " + serverUrl);
            serverFacade = new ServerFacade(serverUrl); // Configura la fachada con la URL dinámica
            gson = new Gson();
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            fail("Server initialization failed. Unable to assign a dynamic port.");
        }
    }

    /**
     * Método de limpieza que se ejecuta una vez después de todas las pruebas.
     * Detiene el servidor para liberar recursos.
     */
    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop();
            System.out.println("Stopped test HTTP server on " + serverUrl);
        }
    }

    /**
     * Método de preparación que se ejecuta antes de cada prueba.
     * Reinicia el token de autenticación y limpia el estado del servidor para evitar interferencias.
     */
    @BeforeEach
    public void setUp() {
        if (serverFacade != null) {
            serverFacade.setAuthToken(null);
        }
    }

    /**
     * Método de limpieza que se ejecuta después de cada prueba.
     * Limpia el estado del servidor para asegurar aislamiento entre pruebas.
     */
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

    /**
     * Prueba que verifica el registro exitoso de un nuevo usuario.
     */
    @Test
    @DisplayName("Test Register Success")
    public void testRegisterSuccess() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade no está inicializado");
        String response = serverFacade.register("testUser", "password123", "test@example.com");
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        assertTrue(jsonResponse.has("authToken"), "La respuesta debe contener un token de autenticación");
        assertNotNull(serverFacade.getAuthToken(), "Debe generarse un token de autenticación");
        System.out.println("Register response: " + response);
    }

    /**
     * Prueba que verifica el fallo al registrar un usuario existente.
     */
    @Test
    @DisplayName("Test Register Failure with Existing User")
    public void testRegisterFailure() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade no está inicializado");
        serverFacade.register("testUser", "password123", "test@example.com");
        assertThrows(IOException.class, () -> serverFacade.register("testUser", "password123", "test2@example.com"),
                "Debe lanzar una excepción al registrar un usuario duplicado");
    }

    /**
     * Prueba que verifica el login exitoso con credenciales válidas.
     */
    @Test
    @DisplayName("Test Login Success")
    public void testLoginSuccess() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade no está inicializado");
        serverFacade.register("testUser", "password123", "test@example.com");
        String response = serverFacade.login("testUser", "password123");
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        assertTrue(jsonResponse.has("authToken"), "La respuesta debe contener un token de autenticación");
        assertNotNull(serverFacade.getAuthToken(), "Debe generarse un token de autenticación");
        System.out.println("Login response: " + response);
    }

    /**
     * Prueba que verifica el fallo al login con credenciales inválidas.
     */
    @Test
    @DisplayName("Test Login Failure with Wrong Password")
    public void testLoginFailure() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade no está inicializado");
        serverFacade.register("testUser", "password123", "test@example.com");
        assertThrows(IOException.class, () -> serverFacade.login("testUser", "wrongPassword"),
                "Debe lanzar una excepción con contraseña incorrecta");
    }

    /**
     * Prueba que verifica el logout exitoso después de un login.
     */
    @Test
    @DisplayName("Test Logout Success")
    public void testLogoutSuccess() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade no está inicializado");
        serverFacade.register("testUser", "password123", "test@example.com");
        serverFacade.login("testUser", "password123");
        String response = serverFacade.logout();
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        assertTrue(jsonResponse.has("message") && jsonResponse.get("message").getAsString().contains("logged out"),
                "La respuesta debe indicar éxito");
        assertNull(serverFacade.getAuthToken(), "El token debe limpiarse tras logout");
        System.out.println("Logout response: " + response);
    }

    /**
     * Prueba que verifica el fallo al logout sin estar autenticado.
     */
    @Test
    @DisplayName("Test Logout Failure without Auth")
    public void testLogoutFailure() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade no está inicializado");
        serverFacade.setAuthToken(null); // Asegura que no hay token
        assertThrows(IOException.class, () -> serverFacade.logout(),
                "Debe lanzar una excepción al intentar logout sin autenticación");
    }

    /**
     * Prueba que verifica la creación exitosa de un nuevo juego.
     */
    @Test
    @DisplayName("Test Create Game Success")
    public void testCreateGameSuccess() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade no está inicializado");
        serverFacade.register("testUser", "password123", "test@example.com");
        serverFacade.login("testUser", "password123");
        String response = serverFacade.createGame("TestGame");
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        assertTrue(jsonResponse.has("gameID"), "La respuesta debe contener el ID del juego");
        System.out.println("Create game response: " + response);
    }

    /**
     * Prueba que verifica la unión exitosa a un juego existente.
     */
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
        } catch (IOException e) {
            System.err.println("Login failed: " + e.getMessage());
            fail("Login should succeed but threw an exception: " + e.getMessage());
        }
        String createGameResponse = null; // Inicialización para evitar error
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
        assertNotNull(joinResponse, "La respuesta de unión a juego no debe ser nula");
        assertTrue(jsonResponse.has("message") && jsonResponse.get("message").getAsString().contains("Joined"),
                "La respuesta debe indicar unión exitosa");
    }

    /**
     * Prueba que verifica el listado exitoso de juegos disponibles.
     */
    @Test
    @DisplayName("Test List Games Success")
    public void testListGamesSuccess() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade no está inicializado");
        serverFacade.register("testUser", "password123", "test@example.com");
        serverFacade.login("testUser", "password123");
        serverFacade.createGame("TestGame");
        String response = serverFacade.listGames();
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        assertTrue(jsonResponse.has("games"), "La respuesta debe contener una lista de juegos");
        System.out.println("List games response: " + response);
    }

    /**
     * Prueba que verifica el fallo al listar juegos sin autenticación.
     */
    @Test
    @DisplayName("Test List Games Failure without Auth")
    public void testListGamesFailure() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade no está inicializado");
        serverFacade.setAuthToken(null); // Asegura que no hay token
        assertThrows(IOException.class, () -> serverFacade.listGames(),
                "Debe lanzar una excepción al listar juegos sin autenticación");
    }

    private String extractGameId(String response) {
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        if (!jsonResponse.has("gameID")) {
            throw new IllegalArgumentException("No gameID found in response: " + response);
        }
        return jsonResponse.get("gameID").getAsString();
    }
}