package dataaccess;

import chess.ChessGame;
import model.AuthData;
import model.GameData;
import model.UserData;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class MemoryDataAccess implements DataAccess {

    private final Map<String, UserData> users = new HashMap<>();
    private final Map<String, AuthData> authTokens = new HashMap<>();
    private final Map<Integer, GameData> games = new HashMap<>();
    private final AtomicInteger gameIdCounter = new AtomicInteger(1); // Reintroducido para gestión de ID en memoria

    public MemoryDataAccess() throws DataAccessException {
        // No hay necesidad de crear tablas en memoria
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        return users.get(username);
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        if (users.containsKey(user.username())) {
            throw new DataAccessException("Error: Ya existe el usuario.");
        }
        users.put(user.username(), user);
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        return authTokens.get(authToken);
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        authTokens.put(auth.authToken(), auth);
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        authTokens.remove(authToken);
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        return games.get(gameID);
    }

    @Override
    public int createGame(GameData game) throws DataAccessException {
        // MODIFICADO: Ahora genera un ID y lo devuelve
        // Ignoramos el gameID que viene en el objeto 'game' si es un placeholder (ej. 0)
        // y asignamos uno nuevo de nuestro contador en memoria.
        int newGameID = gameIdCounter.getAndIncrement();

        // Creamos una nueva instancia de GameData con el ID generado en memoria
        // Esto es importante porque el GameData original podría tener un ID temporal (ej. 0)
        GameData newGameDataWithID = new GameData(
                newGameID,
                game.whiteUsername(),
                game.blackUsername(),
                game.gameName(),
                game.game() // Mantiene la instancia de ChessGame original
        );

        games.put(newGameID, newGameDataWithID);
        return newGameID; // Devuelve el ID generado
    }

    @Override
    public void updateGame(int gameID, GameData game) throws DataAccessException {
        if (!games.containsKey(gameID)) {
            throw new DataAccessException("Juego no encontrado: " + gameID);
        }
        games.put(gameID, game);
    }

    @Override
    public GameData[] getAllGames() throws DataAccessException {
        return games.values().toArray(new GameData[0]);
    }

    // ELIMINADO: generateGameID ya no es parte de la interfaz DataAccess

    @Override
    public void clear() throws DataAccessException {
        users.clear();
        authTokens.clear();
        games.clear();
        gameIdCounter.set(1); // Reiniciar el contador de ID en memoria
    }

    @Override
    public void deleteAllUsers() throws DataAccessException {
        users.clear();
    }

    @Override
    public void deleteAllAuth() throws DataAccessException {
        authTokens.clear();
    }

    @Override
    public void deleteAllGames() throws DataAccessException {
        games.clear();
        gameIdCounter.set(1); // Reiniciar el contador de ID al borrar juegos
    }

    @Override
    public boolean isObserver(int gameID, String username) throws DataAccessException {
        GameData game = getGame(gameID);
        if (game == null) {
            return false;
        }

        // Si el usuario ya es jugador, no es un observador "puro"
        if (Objects.equals(game.whiteUsername(), username) || Objects.equals(game.blackUsername(), username)) {
            return false;
        }

        // En una implementación en memoria, si el usuario existe y no es jugador, se asume que puede ser observador
        UserData user = getUser(username);
        return user != null;
    }
}