package dataaccess;

import model.AuthData;
import model.GameData;
import model.UserData;
import service.Results.Result;

public interface DataAccess {

    UserData getUser(String username) throws DataAccessException;
    void createUser(UserData user) throws DataAccessException;

    AuthData getAuth(String authToken) throws DataAccessException;
    void createAuth(AuthData auth) throws DataAccessException;
    void deleteAuth(String authToken) throws DataAccessException;

    GameData getGame(int gameID) throws DataAccessException;
    int createGame(GameData game) throws DataAccessException; // MODIFICADO: Ahora devuelve el gameID generado
    void updateGame(int gameID, GameData game) throws DataAccessException;
    GameData[] getAllGames() throws DataAccessException;

    // ELIMINADO: generateGameID ya no se usa aquí. La DB lo genera.
    // int generateGameID() throws DataAccessException;

    void clear() throws DataAccessException;

    void deleteAllUsers() throws DataAccessException;
    void deleteAllAuth() throws DataAccessException;
    void deleteAllGames() throws DataAccessException;

    boolean isObserver(int gameID, String username) throws DataAccessException;
}