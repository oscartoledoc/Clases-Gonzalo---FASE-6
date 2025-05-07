package service;

import dataaccess.DataAccessException;
import dataaccess.dataAccess;

import service.Results.*;

public class ClearService{
    private final dataAccess dataaccess;

    public ClearService(dataAccess dataaccess){
        this.dataaccess = dataaccess;
    }
    public Result clear() throws DataAccessException {
        dataaccess.deleteAllUsers();
        dataaccess.deleteAllAuth();
        dataaccess.deleteAllGames();
        return new Result("Cleared all data");
    }

}