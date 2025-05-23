package passoff.server;

import dataaccess.*;
import dataaccess.DataAccessException;
import model.AuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.*;

import static org.junit.jupiter.api.Assertions.*;

public class SessionServiceTests {
    private SessionService sessionService;
    private MemoryDataAccess dataAccess;

    @BeforeEach
    void setUp() throws DataAccessException {
        dataAccess = new MemoryDataAccess(){};
        sessionService = new SessionService(dataAccess);
        try {
            dataAccess.clear();
        } catch (DataAccessException e) {
            fail("Failed to clear data access");
        }
    }

    @Test
    @DisplayName("Logout Success")
    void testLogoutSuccess() throws DataAccessException{
        dataAccess.createAuth(new AuthData("user1", "token123"));
        sessionService.logout("token123");
        assertNull(dataAccess.getAuth("token123"), "Auth should be removed from dataAccess");
    }

    @Test
    @DisplayName("Logout Failure - Invalid Token")
    void testLogoutFailure()throws DataAccessException{
        dataAccess.clear();
        assertThrows(DataAccessException.class, () ->
        {sessionService.logout("invalidToken");},
                "Should throw Exception on Invalid Token");
    }

    @Test
    @DisplayName("Clear Success")
    void testClearSuccess() throws DataAccessException{
        dataAccess.createAuth(new AuthData("user1", "token123"));
        dataAccess.clear();
        assertNull(dataAccess.getAuth("token123"), "Auth should be removed from dataAccess");
    }
}
