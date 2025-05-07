package dataaccess;

import chess.ChessGame;
import model.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public interface dataAccess {
    UserData getUser(String username) throws DataAccessException;
    void createUser(UserData user) throws DataAccessException;
    void createAuth(AuthData auth) throws DataAccessException;
    void deleteAuth(String authToken) throws DataAccessException;

    GameData[] getAllGames() throws DataAccessException;
    GameData getGame(int gameID) throws DataAccessException;
    void createGame(GameData game) throws DataAccessException;
    void updateGame(GameData game) throws DataAccessException;

    void deleteAllUsers() throws DataAccessException;
    void deleteAllAuth() throws DataAccessException;
    void deleteAllGames() throws DataAccessException;
}

public abstract class MemoryDataAccess implements dataAccess {
    private final Map<String, UserData> users = new HashMap<>();
    private final Map<String, AuthData> auths = new HashMap<>();
    private final Map<Integer, GameData> games = new HashMap<>();
    private final AtomicInteger gameIdCounter = new AtomicInteger(1);

    @Override
    public UserData getUser(String username) throws DataAccessException {
        return users.get(username);
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        if (users.containsKey(user.username())) throw new DataAccessException("User already exists");
        users.put(user.username(), user);
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        auths.put(auth.authToken(), auth);
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        auths.remove(authToken);
    }

    @Override
    public GameData[] getAllGames() throws DataAccessException {
        return games.values().toArray(new GameData[0]);
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        return games.get(gameID);
    }

    @Override
    public void createGame(GameData game) throws DataAccessException {
        games.put(game.gameID(), game);
    }

    @Override
    public void updateGame(int gameID, GameData game) throws DataAccessException {
        games.put(gameID, game);
    }



    @Override
    public void deleteAllUsers() throws DataAccessException {
        users.clear();
    }

    @Override
    public void deleteAllAuth() throws DataAccessException {
        auths.clear();
    }

    @Override
    public void deleteAllGames() throws DataAccessException {
        games.clear();
    }

    public int generateGameID() {
        return gameIdCounter.getAndIncrement();
    }


}