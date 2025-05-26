package client;

import org.junit.jupiter.api.*;
import server.Server;
import ui.ServerFacade;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
<<<<<<< HEAD
=======
import static org.junit.jupiter.api.Assumptions.assumeTrue;
>>>>>>> 05d23bd70abf52efa187a1bffe2f2bcb6e48cfc5

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
            serverFacade.setAuthToken(null); // Limpia el token antes de cada prueba
            try {
                // Simula una solicitud para limpiar el estado del servidor (e.g., eliminar usuarios, juegos)
                serverFacade.clearServerState();
            } catch (Exception e) {
                System.err.println("Failed to clear server state: " + e.getMessage());
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
        assertNotNull(response, "La respuesta del registro no debe ser nula");
        assertTrue(response.contains("authToken"), "La respuesta debe contener un token de autenticación");
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
        assertNotNull(response, "La respuesta del login no debe ser nula");
        assertTrue(response.contains("authToken"), "La respuesta debe contener un token de autenticación");
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
        assertNotNull(response, "La respuesta del logout no debe ser nula");
        assertTrue(response.contains("successful"), "La respuesta debe indicar éxito");
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
        assertNotNull(response, "La respuesta de creación de juego no debe ser nula");
        assertTrue(response.contains("gameID") || response.contains("TestGame"),
                "La respuesta debe contener el ID o el nombre del juego");
        System.out.println("Create game response: " + response);
    }

    /**
     * Prueba que verifica la unión exitosa a un juego existente.
     */
    @Test
    @DisplayName("Test Join Game Success")
    public void testJoinGameSuccess() throws IOException {
        assumeTrue(serverFacade != null, "ServerFacade no está inicializado");
        serverFacade.register("testUser", "password123", "test@example.com");
        serverFacade.login("testUser", "password123");
        serverFacade.createGame("TestGame"); // Crea un juego (simula que devuelve gameId = 1)
        String response = serverFacade.joinGame("1", "WHITE"); // Ajusta el gameId según la respuesta
        assertNotNull(response, "La respuesta de unión a juego no debe ser nula");
        assertTrue(response.contains("Joined"), "La respuesta debe indicar unión exitosa");
        System.out.println("Join game response: " + response);
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
        assertNotNull(response, "La respuesta de listado de juegos no debe ser nula");
        assertTrue(response.contains("TestGame"), "La respuesta debe contener el nombre del juego creado");
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
}