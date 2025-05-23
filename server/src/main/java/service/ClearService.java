package service;

import dataaccess.DataAccessException;
import dataaccess.DataAccess;

import service.Results.*;

public class ClearService{
    private final DataAccess dataaccess;

    public ClearService(DataAccess dataaccess){
        this.dataaccess = dataaccess;
    }
    public Result clear() throws DataAccessException {
        dataaccess.deleteAllUsers();
        dataaccess.deleteAllAuth();
        dataaccess.deleteAllGames();
        return new Result("Cleared all data");
    }

}