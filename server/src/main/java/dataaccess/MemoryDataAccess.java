package dataaccess;

import model.AuthData;
import model.GameData;
import model.UserData;
import chess.ChessGame;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;


public class MemoryDataAccess implements DataAccess {
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
        if (users.containsKey(user.username())) {
            throw new DataAccessException("User already exists");
        }
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
        if (games.containsKey(game.gameID())) {
            throw new DataAccessException("Game ID already exists");
        }
        games.put(game.gameID(), game);
    }

    @Override
    public void updateGame(int gameID, GameData game) throws DataAccessException {
        if (!games.containsKey(gameID)) {
            throw new DataAccessException("Game not found for update");
        }
        games.put(gameID, game);
    }

    @Override
    public void clear() throws DataAccessException {
        deleteAllUsers();
        deleteAllAuth();
        deleteAllGames();
        gameIdCounter.set(1);
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

    @Override
    public int generateGameID() throws DataAccessException {
        return gameIdCounter.getAndIncrement();
    }

    @Override
    public boolean isObserver(int gameID, String username) throws DataAccessException {
        GameData game = getGame(gameID);
        if (game == null) {
            return false;
        }

        if (Objects.equals(game.whiteUsername(), username) || Objects.equals(game.blackUsername(), username)) {
            return false;
        }

        UserData user = getUser(username);
        return user != null;
    }
}