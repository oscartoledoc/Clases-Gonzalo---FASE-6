package service;

import dataaccess.DataAccessException;
import dataaccess.dataAccess;
import model.*;

import service.Results.*;

import java.util.UUID;

public class SessionService {
    private final dataAccess dataaccess;

    public SessionService(dataAccess dataaccess) {
        this.dataaccess = dataaccess;
    }

    public Result logout(String authToken) throws DataAccessException {
        dataaccess.deleteAuth(authToken);
        return new Result("Logout successful");
    }

}