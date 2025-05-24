package passoff.server;

import dataaccess.DataAccessException;
import dataaccess.MySQLDataAccess;
import model.UserData;
import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MySQLDataAccessTests {
    private static MySQLDataAccess dataAccess;
    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_PASSWORD = "testPass";
    private static final String TEST_EMAIL = "testMail";
    private static final String TEST_AUTH_TOKEN = "testToken";
    private static final String TEST_GAME_NAME = "testGame";



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

    @Test
    @Order(1)
    @DisplayName("Get User Success")
    public void testCreateUser() throws DataAccessException {
        UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
        dataAccess.createUser(user);
        UserData retrievedUser = dataAccess.getUser(TEST_USERNAME);
        assertNotNull(retrievedUser, "User should be stored in database");
        assertEquals(TEST_USERNAME, retrievedUser.username(), "Username should match");
        assertTrue(BCrypt.checkpw(TEST_PASSWORD, retrievedUser.password()), "Password should match");
    }

    @Test
    @Order(2)
    @DisplayName("Get User Failure - User Not Found")
    public void testGetUserFailure() throws DataAccessException {
        assertNull(dataAccess.getUser("invalidUser"), "User should not be stored in database");
    }

    @Test
    @Order(3)
    @DisplayName("Create User Success")
    public void testCreateUserSuccess() throws DataAccessException {
        UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
        dataAccess.createUser(user);
        UserData retrievedUser = dataAccess.getUser(TEST_USERNAME);
        assertNotNull(retrievedUser, "User should be stored in database");
        assertTrue(BCrypt.checkpw(TEST_PASSWORD, retrievedUser.password()), "Password should match");
    }

    @Test
    @Order(4)
    @DisplayName("Create User Failure - Duplicate Username")
    public void testCreateUserFailure() throws DataAccessException {
        UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
        assertDoesNotThrow(() -> dataAccess.createUser(user), "User creation should be successful" );
        UserData duplicateUser = new UserData(TEST_USERNAME, "differentPass", "differentEmail");
        assertThrows(DataAccessException.class, () ->
        {dataAccess.createUser(user);},
                "Should throw Exception on Duplicate Username");
    }




}
