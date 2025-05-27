package service;
import dataaccess.DataAccessException;
import dataaccess.DataAccess;
import model.*;

import org.mindrot.jbcrypt.BCrypt;
import service.Results.*;
import java.util.UUID;


public class UserService {
    private final DataAccess dataaccess;
    public UserService(DataAccess dataaccess) {
        this.dataaccess = dataaccess;
    }

    public RegisterResult register(String username, String password, String email) throws DataAccessException {
        if (dataaccess.getUser(username) != null) {
            throw new DataAccessException("Username already exists");
        }
        UserData user = new UserData(username, password, email);
        dataaccess.createUser(user);
        String authToken = UUID.randomUUID().toString();

        dataaccess.createAuth(new AuthData(username, authToken));
        return new RegisterResult(username, authToken);
    }

    public RegisterResult login(String username, String password) throws DataAccessException{
        UserData user = dataaccess.getUser(username);
        if (user == null || !BCrypt.checkpw(password, user.password())) {
            throw new DataAccessException("Invalid Credentials");
        }
        String authToken = UUID.randomUUID().toString();
        dataaccess.createAuth(new AuthData(username, authToken));
        return new RegisterResult(username, authToken);
    }

    public void clear() throws DataAccessException {
        dataaccess.deleteAllUsers();
        dataaccess.deleteAllAuth();
    }


}