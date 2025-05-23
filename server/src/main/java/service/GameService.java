package service;
import dataaccess.DataAccessException;
import dataaccess.DataAccess;
import model.*;

import service.Results.*;

public class GameService {

    private final DataAccess dataaccess;
    public GameService(DataAccess dataaccess) {
        this.dataaccess = dataaccess;
    }
    public GameListResult listGames(String authToken) throws DataAccessException{
        if (!isValidAuthToken(authToken)) {
            throw new DataAccessException("Unauthorized");
        }
        return new GameListResult(dataaccess.getAllGames());
    }

    public CreateGameResult createGame(String authToken, String gameName) throws DataAccessException{
        if (!isValidAuthToken(authToken)) {
            throw new DataAccessException("Unauthorized");
        }
        AuthData auth = dataaccess.getAuth(authToken);
        String username = auth.username();
        UserData user = dataaccess.getUser(username);
        if (user == null) {
            throw new DataAccessException("Invalid user");
        }
        int gameID = dataaccess.generateGameID();
        if (gameID < 0) {
            throw new DataAccessException("Unable to generate game ID");
        }
        GameData game = new GameData(gameID, null, null, gameName, null);
        dataaccess.createGame(game);
        return new CreateGameResult(gameID);
    }

    public JoinGameResult joinGame(String authToken, int gameID, String playerColor) throws DataAccessException{
        if (!isValidAuthToken(authToken)) {
            throw new DataAccessException("Unauthorized");
        }
        AuthData auth = dataaccess.getAuth(authToken);
        String username = auth.username();
        UserData user = dataaccess.getUser(username);
        if (user == null) {
            throw new DataAccessException("Invalid user");
        }
        GameData game = dataaccess.getGame(gameID);
        if (game == null) {
            throw new DataAccessException("Invalid game");
        }
        if ("white".equals(playerColor.toLowerCase()) && game.whiteUsername() != null) {
            throw new DataAccessException("Player already joined");
        }
        if ("black".equals(playerColor.toLowerCase()) && game.blackUsername() != null) {
            throw new DataAccessException("Player already joined");
        }

        if (!playerColor.equalsIgnoreCase("white") && !playerColor.equalsIgnoreCase("black")) {
            throw new DataAccessException("Invalid Color");
        }

        game = new GameData(
                gameID,
                "white".equals(playerColor.toLowerCase()) ? username : game.whiteUsername(),
                "black".equals(playerColor.toLowerCase()) ? username : game.blackUsername(),
                game.gameName(),
                game.game());

        dataaccess.updateGame(gameID, game);

        return new JoinGameResult(authToken, gameID, playerColor);
    }

    private boolean isValidAuthToken(String authToken) throws DataAccessException{
        return dataaccess.getAuth(authToken) != null;
    }

}