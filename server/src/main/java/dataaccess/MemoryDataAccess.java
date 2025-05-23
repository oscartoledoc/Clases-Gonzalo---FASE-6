package dataaccess;

import model.AuthData;
import model.GameData;
import model.UserData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public abstract class  MemoryDataAccess implements DataAccess {
    private final Map<String, UserData> users = new HashMap<>();
    private final Map<String, AuthData> auths = new HashMap<>();
    private final Map<Integer, GameData> games = new HashMap<>();
    private final AtomicInteger gameIdCounter = new AtomicInteger(1);

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        return auths.get(authToken);
    }

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
        //return games.values().toArray(new GameData[0]);
        GameData[] gameArray = games.values().stream().filter(game -> game.gameID() != null).toArray(GameData[]::new);
        return gameArray;
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        return games.get(gameID);
    }

    @Override
    public void createGame(GameData game) throws DataAccessException {
        if (game.gameID() == null) throw new DataAccessException("Game ID must be set");
        games.put(game.gameID(), game);
    }

    @Override
    public void updateGame(int gameID, GameData game) throws DataAccessException {
        if (game.gameID() == null) throw new DataAccessException("Game ID must be set");
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
