package service;

import dataaccess.DataAccessException;
import dataaccess.DataAccess;

import model.AuthData;
import service.Results.*;

public class SessionService {
    private final DataAccess dataaccess;

    public SessionService(DataAccess dataaccess) {
        this.dataaccess = dataaccess;
    }

    public Result logout(String authToken) throws DataAccessException {
        if (authToken == null) {
            throw new DataAccessException("Invalid Token");
        }
        AuthData auth = dataaccess.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("Unauthorized");
        }
        dataaccess.deleteAuth(authToken);
        return new Result("Logout successful");
    }



}