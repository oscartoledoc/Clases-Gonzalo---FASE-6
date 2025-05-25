package client;

import org.junit.jupiter.api.*;
import passoff.server.TestServerFacade;
import server.Server;
import ui.ServerFacade;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;


public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade serverFacade;
    private static final String SERVER_URL = "http://localhost:";
    private static int port;

    @BeforeAll
    public static void init() {
        server = new Server();
        port = server.run(0); // Inicia el servidor en un puerto dinámico
        System.out.println("Started test HTTP server on " + port);
        serverFacade = new ServerFacade(SERVER_URL + port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    public void setUp() {
        serverFacade.setAuthToken(null);

    }

    @Test
    @DisplayName("Register Success")
    public void testRegisterSuccess() throws IOException{
        String response = serverFacade.register("TestUsername","TestPassword","TestMail");
        assertNotNull(serverFacade.getAuthToken(), "Auth token was not set");
        assertNotNull(response, "Response was null");
        System.out.println(response);
    }
    @Test
    @DisplayName("Test Register Failure with Existing User")
    public void testRegisterFailure() throws IOException {
        serverFacade.register("testUser", "password123", "test@example.com");
        assertThrows(IOException.class, () -> serverFacade.register("testUser", "password123", "test2@example.com"),
                "Debe lanzar una excepción al registrar un usuario duplicado");
    }

    @Test
    @DisplayName("Test Login Success")
    public void testLoginSuccess() throws IOException{
        serverFacade.register("testUser", "testPassword", "testEmail");
        String response = serverFacade.login("testUser", "testPassword");
        assertNotNull(serverFacade.getAuthToken(), "Auth token was not set");
        assertNotNull(response, "Response was null");
        assertTrue(response.contains("testUser"), "Username was not returned");
        System.out.println(response);
    }

    @Test
    @DisplayName("Test Login Failure with Wrong Password")
    public void testLoginFailure() throws IOException {
        serverFacade.register("testUser", "password123", "test@example.com");
        assertThrows(IOException.class, () -> serverFacade.login("testUser", "wrongPassword"),
                "Debe lanzar una excepción con contraseña incorrecta");
    }




}
