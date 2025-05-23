package service;

import dataaccess.DataAccessException;
import dataaccess.DataAccess;

import service.Results.*;

public class SessionService {
    private final DataAccess dataaccess;

    public SessionService(DataAccess dataaccess) {
        this.dataaccess = dataaccess;
    }

    public Result logout(String authToken) throws DataAccessException {
        dataaccess.deleteAuth(authToken);
        return new Result("Logout successful");
    }

}