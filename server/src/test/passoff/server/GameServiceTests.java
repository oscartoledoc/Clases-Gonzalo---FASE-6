package passoff.server;

import dataaccess.*;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.*;
import service.Results.*;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTests {
    private GameService gameService;
    private MemoryDataAccess dataAccess;

    @BeforeEach
    void setUp() throws DataAccessException {
        dataAccess = new MemoryDataAccess(){};
        gameService = new GameService(dataAccess);
        try {
            dataAccess.clear();
        } catch (DataAccessException e) {
            fail("Failed to clear data access");
        }
    }

    @Test
    @DisplayName("List Games Success")
    void testListGamesSuccess() throws DataAccessException{
        dataAccess.createAuth(new AuthData("user1", "token123"));
        int gameID = dataAccess.generateGameID();
        dataAccess.createGame(new GameData(gameID, null, null, "game1", null));
        GameListResult result = gameService.listGames("token123");
        assertEquals(1, result.games().length, "One game should be returned");
        assertEquals("game1", result.games()[0].gameName(), "Game name should match");
    }

    @Test
    @DisplayName("List Games Failure - Unauthorized")
    void testListGamesFailure() throws DataAccessException{
        assertThrows(DataAccessException.class, () ->
        {gameService.listGames("invalidToken");},
                "Should throw Exception on Unauthorized");
    }

    @Test
    @DisplayName("Create Game Success")
    void testCreateGameSuccess() throws DataAccessException{
        dataAccess.createUser(new UserData("user1", "pass1", "email1"));
        dataAccess.createAuth(new AuthData("user1", "token123"));
        AuthData auth = dataAccess.getAuth("token123");
        if (auth == null) {
            fail("Failed to create auth");
        }
        CreateGameResult result = gameService.createGame("token123", "newGame");
        assertNotNull(result.gameID(), "Game ID should be generated");
        assertNotNull(dataAccess.getGame(result.gameID()), "Game should be stored in dataAccess");

    }

    @Test
    @DisplayName("Create Game Failure")
    void testCreateGameFailure() throws DataAccessException{
        assertThrows(DataAccessException.class, () ->
        {gameService.createGame("invalidToken", "newGame");},
                "Should throw Exception on Invalid Game Name");
    }

    @Test
    @DisplayName("Join Game Success")
    void testJoinGameSuccess() throws DataAccessException{
        dataAccess.createUser(new UserData("user1", "pass1", "email1"));
        dataAccess.createAuth(new AuthData("user1", "token123"));
        int gameID = dataAccess.generateGameID();
        dataAccess.createGame(new GameData(gameID, null, null, "game1", null));
        gameService.joinGame("token123", gameID, "white");
        GameData game = dataAccess.getGame(gameID);
        assertEquals("user1", game.whiteUsername(), "White player should be set");
    }

    @Test
    @DisplayName("Join Game Failure")
    void testJoinGameFailure() throws DataAccessException{
        dataAccess.createUser(new UserData("user1", "pass1", "email1"));
        dataAccess.createAuth(new AuthData("user1", "token123"));
        int gameID = dataAccess.generateGameID();
        dataAccess.createGame(new GameData(gameID, "user2", null, "game1", null));

        assertThrows(DataAccessException.class, () ->
        {gameService.joinGame("token123", gameID, "white");},
                "Should throw Exception for Already Taken Spot");
    }

    @Test
    @DisplayName("Clear Success")
    void testClearSuccess() throws DataAccessException{
        dataAccess.createUser(new UserData("user1", "pass1", "email1"));
        dataAccess.createAuth(new AuthData("user1", "token123"));
        int gameID = dataAccess.generateGameID();
        dataAccess.createGame(new GameData(gameID, null, null, "game1", null));
        dataAccess.clear();
        assertEquals(0, dataAccess.getAllGames().length, "All games should be cleared");
    }


}
