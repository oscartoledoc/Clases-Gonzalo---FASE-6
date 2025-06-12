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
    // Usar ConcurrentHashMap para manejar el estado del juego en memoria de forma segura con hilos
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
        // String username = getUsernameFromAuth(authToken); // Puedes usarlo si el juego requiere el creador

        // Generar un nuevo ChessGame con un tablero reseteado
        ChessGame newChessGame = new ChessGame();
        newChessGame.getBoard().resetBoard();

        // Crear un GameData con un ID temporal (ej. 0) ya que el ID real será generado por la DB
        GameData provisionalGame = new GameData(0, null, null, gameName, newChessGame);

        // MODIFICADO: dataaccess.createGame ahora devuelve el ID generado por la DB
        int gameID = dataaccess.createGame(provisionalGame);

        // Añadir a los juegos activos en memoria con el ID real de la base de datos
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

        // Cargar el juego activo si no está en memoria
        // Si ya está en activeGames, la lambda no se ejecuta. Si no, lo carga y lo pone.
        ChessGame currentChessGame = activeGames.computeIfAbsent(gameID, id -> {
            try {
                GameData storedGame = dataaccess.getGame(id);
                return storedGame != null ? storedGame.game() : new ChessGame();
            } catch (DataAccessException e) {
                System.err.println("Error al cargar el juego " + id + " en activeGames: " + e.getMessage());
                return new ChessGame(); // Devolver un juego vacío en caso de error, aunque es mejor propagar la excepción
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
            // Unirse como observador. No se modifican whiteUsername o blackUsername en GameData.
            // La validación de que el usuario no es ya un jugador es opcional aquí,
            // ya que un jugador puede querer "observar" su propio juego aunque ya sea jugador.
            // Lo más importante es que el WebSocketService maneje que es un observador.
        } else {
            throw new DataAccessException("Solicitud incorrecta: Color inválido");
        }

        // Solo actualizar en la base de datos si hubo un cambio en los jugadores.
        if (updated) {
            GameData updatedGameData = new GameData(
                    gameID,
                    whiteUsername,
                    blackUsername,
                    gameData.gameName(),
                    currentChessGame // Usa la instancia de ChessGame que ya está en activeGames
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

        // Asegurarse de que el juego esté en activeGames
        // Si no está, lo carga de la DB. Si está, lo devuelve.
        ChessGame chessGame = activeGames.computeIfAbsent(gameId, id -> {
            try {
                return dataaccess.getGame(id).game();
            } catch (DataAccessException e) {
                System.err.println("Error al recuperar el estado del juego para el juego " + id + ": " + e.getMessage());
                return new ChessGame(); // Devolver un juego por defecto en caso de error
            }
        });

        // Validar que el usuario sea parte del juego (jugador o futuro observador en WS)
        boolean isPlayer = Objects.equals(gameData.whiteUsername(), username) || Objects.equals(gameData.blackUsername(), username);
        if (!isPlayer && !dataaccess.isObserver(gameId, username)) {
            // Este caso debería ser raro si el flujo de WS es correcto.
            // Si el observador no se persiste en la DB, esta validación debe ser más laxa.
            // Por ahora, asumimos que si el WS ya lo aceptó, está bien.
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
            // Si el juego no está en activeGames (ej. servidor reinició o primer movimiento), cárgalo de la base de datos
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

        // Antes de makeMove, validar si el movimiento es legal para la pieza y el tablero actual
        // El método validMoves() de ChessGame ya hace la verificación de dejar al rey en jaque
        Collection<ChessMove> possibleMoves = chessGame.validMoves(move.getStartPosition());
        boolean isLegalMove = false;
        for (ChessMove validMove : possibleMoves) {
            if (validMove.equals(move)) { // Compara los objetos ChessMove, incluyendo la promoción
                isLegalMove = true;
                break;
            }
        }

        if (!isLegalMove) {
            throw new InvalidMoveException("Movimiento inválido: No es un movimiento legal para esta pieza o deja al rey en jaque.");
        }

        // Realizar el movimiento
        chessGame.makeMove(move);

        // Actualizar el juego en la base de datos
        GameData updatedGameData = new GameData(
                gameId,
                gameData.whiteUsername(),
                gameData.blackUsername(),
                gameData.gameName(),
                chessGame // Usa la instancia de ChessGame que está en activeGames y ha sido modificada
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
            // Si el juego no está en activeGames, cárgalo de la base de datos
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

        chessGame.setGameOver(true); // Marca el juego como terminado

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
            // Si el juego no está en activeGames, cárgalo de la base de datos
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

        // Si el usuario que se va era un jugador, actualiza la base de datos.
        if (removedFromPlayerRole) {
            GameData updatedGameData = new GameData(
                    gameId,
                    whiteUsername,
                    blackUsername,
                    gameData.gameName(),
                    chessGame // El estado del juego en sí no cambia al dejar
            );
            dataaccess.updateGame(gameId, updatedGameData);
        } else {
            // Si no era un jugador y no hay una forma persistente de manejar observadores,
            // no se requiere actualización de DB en este caso. La desconexión WS se maneja por separado.
        }
    }


    public void clear() throws DataAccessException {
        dataaccess.clear();
        activeGames.clear(); // Limpiar también los juegos en memoria
    }
}