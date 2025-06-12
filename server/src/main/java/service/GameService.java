package service;

import dataaccess.DataAccessException;
import dataaccess.DataAccess;
import model.*;

import service.Results.*;
import chess.*;
import chess.InvalidMoveException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class GameService {

    private final DataAccess dataaccess;
    private final Map<Integer, ChessGame> activeGames = new ConcurrentHashMap<>();

    public GameService(DataAccess dataaccess) {
        this.dataaccess = dataaccess;
    }

    private boolean isValidAuthToken(String authToken) throws DataAccessException {
        return dataaccess.getAuth(authToken) != null;
    }

    public String getUsernameFromAuth(String authToken) throws DataAccessException {
        AuthData authData = dataaccess.getAuth(authToken);
        if (authData == null) {
            throw new DataAccessException("No autorizado: Token de autenticación inválido.");
        }
        return authData.username();
    }

    public GameListResult listGames(String authToken) throws DataAccessException {
        if (!isValidAuthToken(authToken)) {
            throw new DataAccessException("No autorizado");
        }
        return new GameListResult(dataaccess.getAllGames());
    }

    public CreateGameResult createGame(String authToken, String gameName) throws DataAccessException {
        if (!isValidAuthToken(authToken)) {
            throw new DataAccessException("No autorizado");
        }

        ChessGame newChessGame = new ChessGame();
        newChessGame.getBoard().resetBoard();

        GameData provisionalGame = new GameData(0, null, null, gameName, newChessGame);

        int gameID = dataaccess.createGame(provisionalGame);

        activeGames.put(gameID, newChessGame);

        return new CreateGameResult(gameID);
    }

    public JoinGameResult joinGame(String authToken, int gameID, String playerColor) throws DataAccessException {
        if (!isValidAuthToken(authToken)) {
            throw new DataAccessException("No autorizado");
        }
        String username = getUsernameFromAuth(authToken);

        GameData gameData = dataaccess.getGame(gameID);
        if (gameData == null) {
            throw new DataAccessException("ID de juego incorrecto: Juego inválido");
        }


        ChessGame currentChessGame = activeGames.computeIfAbsent(gameID, id -> {
            try {
                GameData storedGame = dataaccess.getGame(id);
                return storedGame != null ? storedGame.game() : new ChessGame();
            } catch (DataAccessException e) {
                System.err.println("Error al cargar el juego " + id + " en activeGames: " + e.getMessage());
                return new ChessGame();
            }
        });

        String whiteUsername = gameData.whiteUsername();
        String blackUsername = gameData.blackUsername();

        boolean updated = false;

        if ("white".equalsIgnoreCase(playerColor)) {
            if (whiteUsername != null && !whiteUsername.equals(username)) {
                throw new DataAccessException("Ya tomado");
            }
            if (whiteUsername == null) {
                whiteUsername = username;
                updated = true;
            }
        } else if ("black".equalsIgnoreCase(playerColor)) {
            if (blackUsername != null && !blackUsername.equals(username)) {
                throw new DataAccessException("Ya tomado");
            }
            if (blackUsername == null) {
                blackUsername = username;
                updated = true;
            }
        } else if (playerColor == null || "observer".equalsIgnoreCase(playerColor)) {

        } else {
            throw new DataAccessException("Solicitud incorrecta: Color inválido");
        }

        if (updated) {
            GameData updatedGameData = new GameData(
                    gameID,
                    whiteUsername,
                    blackUsername,
                    gameData.gameName(),
                    currentChessGame
            );
            dataaccess.updateGame(gameID, updatedGameData);
        }

        return new JoinGameResult(authToken, gameID, playerColor);
    }

    public ChessGame getGameState(int gameId, String authToken) throws DataAccessException {
        if (!isValidAuthToken(authToken)) {
            throw new DataAccessException("No autorizado");
        }
        String username = getUsernameFromAuth(authToken);

        GameData gameData = dataaccess.getGame(gameId);
        if (gameData == null) {
            throw new DataAccessException("ID de juego incorrecto: Juego inválido");
        }


        ChessGame chessGame = activeGames.computeIfAbsent(gameId, id -> {
            try {
                return dataaccess.getGame(id).game();
            } catch (DataAccessException e) {
                System.err.println("Error al recuperar el estado del juego para el juego " + id + ": " + e.getMessage());
                return new ChessGame();
            }
        });

        boolean isPlayer = Objects.equals(gameData.whiteUsername(), username) || Objects.equals(gameData.blackUsername(), username);
        if (!isPlayer && !dataaccess.isObserver(gameId, username)) {

        }

        return chessGame;
    }

    public void makeMove(int gameId, String authToken, ChessMove move) throws DataAccessException, InvalidMoveException {
        if (!isValidAuthToken(authToken)) {
            throw new DataAccessException("No autorizado");
        }
        String username = getUsernameFromAuth(authToken);

        GameData gameData = dataaccess.getGame(gameId);
        if (gameData == null) {
            throw new DataAccessException("ID de juego incorrecto: Juego inválido");
        }

        ChessGame chessGame = activeGames.get(gameId);
        if (chessGame == null) {
            chessGame = gameData.game();
            if (chessGame == null) {
                throw new DataAccessException("El estado del juego está corrupto.");
            }
            activeGames.put(gameId, chessGame);
        }

        if (chessGame.isGameOver()) {
            throw new InvalidMoveException("Solicitud incorrecta: No se puede hacer el movimiento: El juego ya ha terminado.");
        }

        ChessGame.TeamColor playerColor = null;
        if (Objects.equals(gameData.whiteUsername(), username)) {
            playerColor = ChessGame.TeamColor.WHITE;
        } else if (Objects.equals(gameData.blackUsername(), username)) {
            playerColor = ChessGame.TeamColor.BLACK;
        } else {
            throw new DataAccessException("No autorizado: Solo los jugadores pueden hacer movimientos.");
        }

        if (chessGame.getTeamTurn() != playerColor) {
            throw new InvalidMoveException("Solicitud incorrecta: ¡No es tu turno!");
        }

        Collection<ChessMove> possibleMoves = chessGame.validMoves(move.getStartPosition());
        boolean isLegalMove = false;
        for (ChessMove validMove : possibleMoves) {
            if (validMove.equals(move)) {
                isLegalMove = true;
                break;
            }
        }

        if (!isLegalMove) {
            throw new InvalidMoveException("Movimiento inválido: No es un movimiento legal para esta pieza o deja al rey en jaque.");
        }

        chessGame.makeMove(move);

        GameData updatedGameData = new GameData(
                gameId,
                gameData.whiteUsername(),
                gameData.blackUsername(),
                gameData.gameName(),
                chessGame
        );
        dataaccess.updateGame(gameId, updatedGameData);
    }

    public void resign(int gameId, String authToken) throws DataAccessException {
        if (!isValidAuthToken(authToken)) {
            throw new DataAccessException("No autorizado");
        }
        String username = getUsernameFromAuth(authToken);

        GameData gameData = dataaccess.getGame(gameId);
        if (gameData == null) {
            throw new DataAccessException("ID de juego incorrecto: Juego inválido");
        }

        ChessGame chessGame = activeGames.get(gameId);
        if (chessGame == null) {
            chessGame = gameData.game();
            if (chessGame == null) {
                throw new DataAccessException("El estado del juego está corrupto.");
            }
            activeGames.put(gameId, chessGame);
        }

        boolean isPlayer = Objects.equals(gameData.whiteUsername(), username) || Objects.equals(gameData.blackUsername(), username);
        if (!isPlayer) {
            throw new DataAccessException("Prohibido: Solo los jugadores pueden renunciar a un juego.");
        }

        if (chessGame.isGameOver()) {
            throw new DataAccessException("Solicitud incorrecta: No se puede renunciar: El juego ya ha terminado.");
        }

        chessGame.setGameOver(true);

        GameData updatedGameData = new GameData(
                gameId,
                gameData.whiteUsername(),
                gameData.blackUsername(),
                gameData.gameName(),
                chessGame
        );
        dataaccess.updateGame(gameId, updatedGameData);
    }

    public void leaveGame(int gameId, String authToken) throws DataAccessException {
        if (!isValidAuthToken(authToken)) {
            throw new DataAccessException("No autorizado");
        }
        String username = getUsernameFromAuth(authToken);

        GameData gameData = dataaccess.getGame(gameId);
        if (gameData == null) {
            throw new DataAccessException("ID de juego incorrecto: Juego inválido");
        }

        ChessGame chessGame = activeGames.get(gameId);
        if (chessGame == null) {
            chessGame = gameData.game();
            if (chessGame == null) {
                throw new DataAccessException("El estado del juego está corrupto.");
            }
            activeGames.put(gameId, chessGame);
        }

        String whiteUsername = gameData.whiteUsername();
        String blackUsername = gameData.blackUsername();
        boolean removedFromPlayerRole = false;

        if (Objects.equals(whiteUsername, username)) {
            whiteUsername = null;
            removedFromPlayerRole = true;
        }
        if (Objects.equals(blackUsername, username)) {
            blackUsername = null;
            removedFromPlayerRole = true;
        }


        if (removedFromPlayerRole) {
            GameData updatedGameData = new GameData(
                    gameId,
                    whiteUsername,
                    blackUsername,
                    gameData.gameName(),
                    chessGame
            );
            dataaccess.updateGame(gameId, updatedGameData);
        } else {

        }
    }


    public void clear() throws DataAccessException {
        dataaccess.clear();
        activeGames.clear();
    }
}