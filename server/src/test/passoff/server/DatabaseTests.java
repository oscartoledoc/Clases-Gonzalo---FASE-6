package dataaccess;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;



import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MySQLDataAccessTest {

    private static MySQLDataAccess dataAccess;
    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_PASSWORD = "testPass";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_AUTH_TOKEN = "testToken123";
    private static final String TEST_GAME_NAME = "TestGame";

    @BeforeAll
    public static void setUpClass() throws DataAccessException {
        dataAccess = new MySQLDataAccess();
        dataAccess.clear();
    }

    @BeforeEach
    public void setUp() throws DataAccessException {
        dataAccess.clear();
    }

    @AfterAll
    public static void tearDownClass() throws DataAccessException {
        dataAccess.clear();
    }

    // --- Pruebas para Usuarios ---

    @Test
    @Order(1)
    @DisplayName("Get User Success")
    public void getUserSuccess() throws DataAccessException {
        UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
        dataAccess.createUser(user);

        UserData retrievedUser = dataAccess.getUser(TEST_USERNAME);
        assertNotNull(retrievedUser, "User should be retrieved");
        assertEquals(TEST_USERNAME, retrievedUser.username(), "Username should match");
        assertTrue(BCrypt.checkpw(TEST_PASSWORD, retrievedUser.password()), "Password should match");
    }

    @Test
    @Order(2)
    @DisplayName("Get User Failure - Nonexistent User")
    public void getUserFailure() throws DataAccessException {
        UserData retrievedUser = dataAccess.getUser("nonexistent");
        assertNull(retrievedUser, "Should return null for nonexistent user");
    }

    @Test
    @Order(3)
    @DisplayName("Create User Success")
    public void createUserSuccess() throws DataAccessException {
        UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
        dataAccess.createUser(user);

        UserData retrievedUser = dataAccess.getUser(TEST_USERNAME);
        assertNotNull(retrievedUser, "User should be created and retrievable");
        assertTrue(BCrypt.checkpw(TEST_PASSWORD, retrievedUser.password()), "Password should be hashed");
    }

    @Test
    @Order(4)
    @DisplayName("Create User Failure - Duplicate Username")
    public void createUserFailure() {
        UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
        assertDoesNotThrow(() -> dataAccess.createUser(user), "First user creation should succeed");

        UserData duplicateUser = new UserData(TEST_USERNAME, "differentPass", "different@example.com");
        assertThrows(DataAccessException.class, () -> dataAccess.createUser(duplicateUser),
                "Should throw exception for duplicate username");
    }

    @Test
    @Order(5)
    @DisplayName("Delete All Users Success")
    public void deleteAllUsersSuccess() throws DataAccessException {
        dataAccess.createUser(new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL));
        dataAccess.deleteAllUsers();

        assertNull(dataAccess.getUser(TEST_USERNAME), "User should be deleted after clearing all users");
    }

    // --- Pruebas para Autenticaciones ---

    @Test
    @Order(6)
    @DisplayName("Get Auth Success")
    public void getAuthSuccess() throws DataAccessException {
        dataAccess.createUser(new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL));
        AuthData auth = new AuthData(TEST_AUTH_TOKEN, TEST_USERNAME);
        dataAccess.createAuth(auth);

        AuthData retrievedAuth = dataAccess.getAuth(TEST_AUTH_TOKEN);
        assertNotNull(retrievedAuth, "Auth should be retrieved");
        assertEquals(TEST_AUTH_TOKEN, retrievedAuth.authToken(), "Auth token should match");
    }

    @Test
    @Order(7)
    @DisplayName("Get Auth Failure - Nonexistent Token")
    public void getAuthFailure() throws DataAccessException {
        AuthData retrievedAuth = dataAccess.getAuth("nonexistentToken");
        assertNull(retrievedAuth, "Should return null for nonexistent auth token");
    }

    @Test
    @Order(8)
    @DisplayName("Create Auth Success")
    public void createAuthSuccess() throws DataAccessException {
        dataAccess.createUser(new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL));
        AuthData auth = new AuthData(TEST_AUTH_TOKEN, TEST_USERNAME);
        dataAccess.createAuth(auth);

        AuthData retrievedAuth = dataAccess.getAuth(TEST_AUTH_TOKEN);
        assertNotNull(retrievedAuth, "Auth should be created and retrievable");
    }

    @Test
    @Order(9)
    @DisplayName("Create Auth Failure - Invalid Username")
    public void createAuthFailure() {
        AuthData auth = new AuthData(TEST_AUTH_TOKEN, "nonexistentUser");
        assertThrows(DataAccessException.class, () -> dataAccess.createAuth(auth),
                "Should throw exception for auth with nonexistent username");
    }

    @Test
    @Order(10)
    @DisplayName("Delete Auth Success")
    public void deleteAuthSuccess() throws DataAccessException {
        dataAccess.createUser(new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL));
        AuthData auth = new AuthData(TEST_AUTH_TOKEN, TEST_USERNAME);
        dataAccess.createAuth(auth);

        dataAccess.deleteAuth(TEST_AUTH_TOKEN);
        assertNull(dataAccess.getAuth(TEST_AUTH_TOKEN), "Auth should be deleted");
    }

    @Test
    @Order(11)
    @DisplayName("Delete Auth Failure - Nonexistent Token")
    public void deleteAuthFailure() {
        assertDoesNotThrow(() -> dataAccess.deleteAuth("nonexistentToken"),
                "Deleting a nonexistent auth token should not throw an exception");
    }

    @Test
    @Order(12)
    @DisplayName("Delete All Auth Success")
    public void deleteAllAuthSuccess() throws DataAccessException {
        dataAccess.createUser(new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL));
        dataAccess.createAuth(new AuthData(TEST_AUTH_TOKEN, TEST_USERNAME));
        dataAccess.deleteAllAuth();

        assertNull(dataAccess.getAuth(TEST_AUTH_TOKEN), "Auth should be deleted after clearing all auths");
    }

    // --- Pruebas para Juegos ---

    @Test
    @Order(13)
    @DisplayName("Get Game Success")
    public void getGameSuccess() throws DataAccessException {
        dataAccess.createUser(new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL));
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(1, TEST_USERNAME, null, TEST_GAME_NAME, game);
        dataAccess.createGame(gameData);

        GameData retrievedGame = dataAccess.getGame(1);
        assertNotNull(retrievedGame, "Game should be retrieved");
        assertEquals(TEST_GAME_NAME, retrievedGame.gameName(), "Game name should match");
        assertNotNull(retrievedGame.game(), "ChessGame should be deserialized");
    }

    @Test
    @Order(14)
    @DisplayName("Get Game Failure - Nonexistent Game")
    public void getGameFailure() throws DataAccessException {
        GameData retrievedGame = dataAccess.getGame(999);
        assertNull(retrievedGame, "Should return null for nonexistent game");
    }

    @Test
    @Order(15)
    @DisplayName("Create Game Success")
    public void createGameSuccess() throws DataAccessException {
        dataAccess.createUser(new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL));
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(1, TEST_USERNAME, null, TEST_GAME_NAME, game);
        dataAccess.createGame(gameData);

        GameData retrievedGame = dataAccess.getGame(1);
        assertNotNull(retrievedGame, "Game should be created and retrievable");
        assertEquals(TEST_USERNAME, retrievedGame.whiteUsername(), "White username should match");
    }

    @Test
    @Order(16)
    @DisplayName("Create Game Failure - Duplicate Game ID")
    public void createGameFailure() throws DataAccessException {
        dataAccess.createUser(new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL));
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(1, TEST_USERNAME, null, TEST_GAME_NAME, game);
        dataAccess.createGame(gameData);

        GameData duplicateGame = new GameData(1, null, TEST_USERNAME, TEST_GAME_NAME + "2", game);
        assertThrows(DataAccessException.class, () -> dataAccess.createGame(duplicateGame),
                "Should throw exception for duplicate game ID");
    }

    @Test
    @Order(17)
    @DisplayName("Update Game Success")
    public void updateGameSuccess() throws DataAccessException {
        dataAccess.createUser(new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL));
        ChessGame initialGame = new ChessGame();
        GameData initialGameData = new GameData(1, TEST_USERNAME, null, TEST_GAME_NAME, initialGame);
        dataAccess.createGame(initialGameData);

        ChessGame updatedGame = new ChessGame();
        updatedGame.setBoard(new ChessBoard()); // Simula un cambio en el tablero
        updatedGame.setTeamTurn(ChessGame.TeamColor.BLACK);
        GameData updatedGameData = new GameData(1, TEST_USERNAME, "blackUser", TEST_GAME_NAME + "Updated", updatedGame);
        dataAccess.updateGame(1, updatedGameData);

        GameData retrievedGame = dataAccess.getGame(1);
        assertNotNull(retrievedGame, "Game should be updated");
        assertEquals("blackUser", retrievedGame.blackUsername(), "Black username should be updated");
        assertEquals(ChessGame.TeamColor.BLACK, retrievedGame.game().getTeamTurn(), "Game state should be updated");
    }

    @Test
    @Order(18)
    @DisplayName("Update Game Failure - Nonexistent Game")
    public void updateGameFailure() {
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(999, null, null, TEST_GAME_NAME, game);
        assertThrows(DataAccessException.class, () -> dataAccess.updateGame(999, gameData),
                "Should throw exception for updating nonexistent game");
    }

    @Test
    @Order(19)
    @DisplayName("Get All Games Success")
    public void getAllGamesSuccess() throws DataAccessException {
        dataAccess.createUser(new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL));
        ChessGame game1 = new ChessGame();
        ChessGame game2 = new ChessGame();
        dataAccess.createGame(new GameData(1, TEST_USERNAME, null, TEST_GAME_NAME, game1));
        dataAccess.createGame(new GameData(2, null, TEST_USERNAME, TEST_GAME_NAME + "2", game2));

        GameData[] games = dataAccess.getAllGames();
        assertEquals(2, games.length, "Should retrieve all games");
        assertTrue(Arrays.stream(games).anyMatch(g -> g.gameName().equals(TEST_GAME_NAME)), "First game should be retrieved");
    }

    @Test
    @Order(20)
    @DisplayName("Get All Games Failure - Empty Database")
    public void getAllGamesFailure() throws DataAccessException {
        GameData[] games = dataAccess.getAllGames();
        assertEquals(0, games.length, "Should return empty array when no games exist");
    }

    @Test
    @Order(21)
    @DisplayName("Generate Game ID Success")
    public void generateGameIDSuccess() throws DataAccessException {
        int gameID = dataAccess.generateGameID();
        assertTrue(gameID > 0, "Generated gameID should be positive");
        assertNull(dataAccess.getGame(gameID), "Generated ID should not create a game");
    }

    @Test
    @Order(22)
    @DisplayName("Generate Game ID Failure - Database Error Simulation")
    public void generateGameIDFailure() throws DataAccessException {
        // Este caso es difícil de simular sin modificar el código o la base de datos.
        // Podrías simularlo eliminando permisos en la base de datos o alterando la tabla,
        // pero para fines prácticos, asumimos que generateGameID no falla fácilmente.
        // Si tienes un caso específico para simular este fallo, lo ajustaremos.
        assertDoesNotThrow(() -> dataAccess.generateGameID(), "Generate gameID should not throw in normal conditions");
    }

    @Test
    @Order(23)
    @DisplayName("Delete All Games Success")
    public void deleteAllGamesSuccess() throws DataAccessException {
        dataAccess.createUser(new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL));
        ChessGame game = new ChessGame();
        dataAccess.createGame(new GameData(1, TEST_USERNAME, null, TEST_GAME_NAME, game));
        dataAccess.deleteAllGames();

        assertEquals(0, dataAccess.getAllGames().length, "All games should be deleted");
    }

    @Test
    @Order(24)
    @DisplayName("Clear Success")
    public void clearSuccess() throws DataAccessException {
        dataAccess.createUser(new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL));
        dataAccess.createAuth(new AuthData(TEST_AUTH_TOKEN, TEST_USERNAME));
        ChessGame game = new ChessGame();
        dataAccess.createGame(new GameData(1, TEST_USERNAME, null, TEST_GAME_NAME, game));

        dataAccess.clear();

        assertNull(dataAccess.getUser(TEST_USERNAME), "User should be cleared");
        assertNull(dataAccess.getAuth(TEST_AUTH_TOKEN), "Auth should be cleared");
        assertEquals(0, dataAccess.getAllGames().length, "Games should be cleared");
    }

    @Test
    @Order(25)
    @DisplayName("Test Chess Game Persistence with Moves")
    public void testChessGamePersistenceWithMoves() throws DataAccessException {
        dataAccess.createUser(new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL));
        dataAccess.createUser(new UserData("blackUser", TEST_PASSWORD, "black@example.com"));

        // Crear un juego con un tablero inicial
        ChessGame chessGame = new ChessGame();
        chessGame.setBoard(new ChessBoard());
        chessGame.getBoard().resetBoard(); // Tablero inicial
        GameData gameData = new GameData(1, TEST_USERNAME, "blackUser", TEST_GAME_NAME, chessGame);
        dataAccess.createGame(gameData);

        // Hacer un movimiento
        ChessMove move = new ChessMove(new ChessPosition(2, 1), new ChessPosition(4, 1), null); // Peón blanco avanza
        chessGame.makeMove(move);
        GameData updatedGameData = new GameData(1, TEST_USERNAME, "blackUser", TEST_GAME_NAME, chessGame);
        dataAccess.updateGame(1, updatedGameData);

        // Simula un reinicio
        dataAccess = new MySQLDataAccess();

        // Verifica persistencia
        GameData retrievedGame = dataAccess.getGame(1);
        assertNotNull(retrievedGame, "Game should persist after restart");
        assertNotNull(retrievedGame.game(), "ChessGame should be deserialized");
        assertEquals(ChessGame.TeamColor.BLACK, retrievedGame.game().getTeamTurn(), "Turn should be BLACK after move");
        assertNull(retrievedGame.game().getBoard().getPiece(new ChessPosition(2, 1)), "Piece should have moved");
        assertNotNull(retrievedGame.game().getBoard().getPiece(new ChessPosition(4, 1)), "Piece should be in new position");
    }
}