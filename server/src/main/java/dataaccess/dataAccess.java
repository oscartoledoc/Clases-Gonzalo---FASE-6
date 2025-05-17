package dataaccess;

import chess.ChessGame;
import model.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public interface dataAccess {
    UserData getUser(String username) throws DataAccessException;
    void createUser(UserData user) throws DataAccessException;
    AuthData getAuth(String authToken) throws DataAccessException;
    void createAuth(AuthData auth) throws DataAccessException;
    void deleteAuth(String authToken) throws DataAccessException;

    GameData[] getAllGames() throws DataAccessException;
    GameData getGame(int gameID) throws DataAccessException;
    void createGame(GameData game) throws DataAccessException;
    void updateGame(int gameID, GameData game) throws DataAccessException;

    void clear() throws DataAccessException;
    void deleteAllUsers() throws DataAccessException;
    void deleteAllAuth() throws DataAccessException;
    void deleteAllGames() throws DataAccessException;

    int generateGameID() throws DataAccessException;
}





