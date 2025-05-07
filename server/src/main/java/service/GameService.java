package service;
import dataaccess.DataAccessException;
import dataaccess.dataAccess;
import model.*;

import service.Results.*;

public class GameService {

    private final dataAccess dataaccess;
    public GameService(dataAccess dataaccess) {
        this.dataaccess = dataaccess;
    }
    public GameListResult listGames(String authToken) throws DataAccessException{
        if (!isValidAuthToken(authToken)) throw new DataAccessException("Unauthorized");
        return new GameListResult(dataaccess.getAllGames());
    }

    public CreateGameResult createGame(String authToken, String gameName) throws DataAccessException{
        if (!isValidAuthToken(authToken)) throw new DataAccessException("Unauthorized");
        String username = dataaccess.getUser(authToken).username();
        int gameID = dataaccess.generateGameID();
        GameData game = new GameData(gameID, null, gameName, null, null);
        dataaccess.createGame(game);
        return new CreateGameResult(gameID);
    }

    public JoinGameResult joinGame(String authToken, int gameID, String playerColor) throws DataAccessException{
        if (!isValidAuthToken(authToken)) throw new DataAccessException("Unauthorized");
        String username = dataaccess.getUser(authToken).username();
        GameData game = dataaccess.getGame(gameID);
        if (game == null) throw new DataAccessException("Invalid game");
        if ("White".equals(playerColor) && game.whiteUsername() != null) throw new DataAccessException("Player already joined");
        if ("Black".equals(playerColor) && game.blackUsername() != null) throw new DataAccessException("Player already joined");

        game = new GameData(gameID, "White".equals(playerColor) ? username : game.whiteUsername(), "Black".equals(playerColor) ? username : game.blackUsername(), game.gameName(), game.game());

        dataaccess.updateGame(gameID, game);

        return new JoinGameResult(authToken, gameID, playerColor);
    }

    private boolean isValidAuthToken(String authToken) throws DataAccessException{
        return dataaccess.getAuth(authToken) != null;
    }

}
