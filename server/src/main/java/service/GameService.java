package service;
import dataaccess.DataAccessException;
import dataaccess.DataAccess;
import model.*;

import service.Results.*;
import chess.*;

public class GameService {

    private final DataAccess dataaccess;
    private final java.util.Map<Integer, ChessGame> activeGames = new java.util.HashMap<>();
    private final com.google.gson.Gson gson = new com.google.gson.Gson();
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

    public String getGameState(int gameId, String authToken) throws DataAccessException {
        if (!isValidAuthToken(authToken)) {
            throw new DataAccessException("Unauthorized");
        }
        GameData game = dataaccess.getGame(gameId);
        if (game == null || !activeGames.containsKey(gameId)) {
            throw new DataAccessException("Invalid game");
        }
        ChessGame chessGame = activeGames.get(gameId);
        return gson.toJson(chessGame.getBoard());
    }

    public void makeMove(int gameId, String authToken, ChessMove move) throws Exception {
        if (!isValidAuthToken(authToken)) {
            throw new DataAccessException("Unauthorized");
        }
        ChessGame game = activeGames.get(gameId);
        if (game == null) {
            throw new DataAccessException("Invalid game");
        }
        AuthData auth = dataaccess.getAuth(authToken);
        String username = auth.username();
        GameData gameData = dataaccess.getGame(gameId);
        ChessGame.TeamColor playerColor = gameData.whiteUsername().equals(username) ? ChessGame.TeamColor.WHITE : ChessGame.TeamColor.BLACK;

        if (!game.getTeamTurn().equals(playerColor)) {
            throw new Exception("Not your turn");
        }
        if (game.isGameOver()) {
            throw new Exception("Game is over");
        }
        if (!game.validMoves((move.getStartPosition()).contains(move))) {
            throw new Exception("Invalid move");
        }
        game.makeMove(move);

        if (game.isInCheckmate(playerColor.opposite())) {
            game.setGameOver(true);
            throw new Exception("Checkmate! Game over.");
        } else if (game.isInStalemate(playerColor.opposite())) {
            game.setGameOver(true);
            throw new Exception("Stalemate! Game over.");
        } else if (game.isInCheck(playerColor)) {
            throw new Exception("You are in check!");
        }
    }

    public void resign(int gameId, String authToken) throws DataAccessException {
        if (!isValidAuthToken(authToken)) {
            throw new DataAccessException("Unauthorized");
        }
        ChessGame game = activeGames.get(gameId);
        if (game == null) {
            throw new DataAccessException("Invalid game");
        }
        game.setGameOver(true);
        GameData gameData = dataaccess.getGame(gameId);
        dataaccess.updateGame(gameId, new GameData(gameId, gameData.whiteUsername(), gameData.blackUsername(), gameData.gameName(), game));
    }

    private boolean isValidAuthToken(String authToken) throws DataAccessException{
        return dataaccess.getAuth(authToken) != null;
    }

}