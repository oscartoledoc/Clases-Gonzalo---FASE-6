package passoff.server;

import dataaccess.*;
import dataaccess.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.*;
import service.Results.*;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {
    private UserService userService;
    private MemoryDataAccess dataAccess;

    @BeforeEach
    void setUp() throws DataAccessException {
        dataAccess = new MemoryDataAccess(){};
        userService = new UserService(dataAccess);
        try {
            userService.clear();
        } catch (DataAccessException e) {
            fail("Failed to clear data access");
        }
    }

    @Test
    @DisplayName("Register Success")
    void testRegisterSuccess() throws DataAccessException{
      Results.RegisterResult result = userService.register("user1", "pass1", "email1");
      assertNotNull(result.authToken(), "AuthToken should be generated");
      assertEquals("user1", result.username(), "Username should match");
      assertNotNull(dataAccess.getUser("user1"), "User should be stored in dataAccess");
    }

    @Test
    @DisplayName("Register Duplicate")
    void testRegisterDuplicate() throws DataAccessException{
      userService.register("user1", "pass1", "email1");
      assertThrows(DataAccessException.class, () ->
      {userService.register("user1", "pass2", "email2");},
              "Should throw Exception on Duplicate Username");
    }

    @Test
    @DisplayName("Login Success")
    void testLoginSuccess() throws DataAccessException{
        userService.register("user1", "pass1", "email1");
        RegisterResult result = userService.login("user1", "pass1");
        assertNotNull(result.authToken(), "AuthToken should be generated");
        assertEquals("user1", result.username(), "Username should match");
    }

    @Test
    @DisplayName("Login Failure - Wrong Password")
    void testLoginFailure() throws DataAccessException{
        userService.register("user1", "pass1", "email1");
        assertThrows(DataAccessException.class, () ->
        {userService.login("user1", "wrongPass");},
                "Should throw Exception on Invalid Password");
    }

    @Test
    @DisplayName("Clear Success")
    void testClearSuccess() throws DataAccessException{
        userService.register("user1", "pass1", "email1");
        userService.clear();
        assertNull(dataAccess.getUser("user1"), "User should be removed from dataAccess");
        assertNull(dataAccess.getAuth("token123"), "AuthTokens should be cleared");
    }
}
