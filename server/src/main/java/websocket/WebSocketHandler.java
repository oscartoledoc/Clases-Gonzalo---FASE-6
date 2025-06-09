package websocket; // Reconfirmando el paquete

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import service.GameService;
import dataaccess.DataAccessException;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

// ¡IMPORTANTE! Aquí se mantienen las importaciones que coinciden con tus paquetes
import websocket.commands.UserGameCommand;
import websocket.commands.MakeMoveCommand;
import websocket.messages.ServerMessageError; // Usar tus nombres de clases concretas
import websocket.messages.LoadGameMessage;
import websocket.messages.ServerMessageNotification;
import websocket.messages.ServerMessage; // La clase base ServerMessage

import chess.ChessGame;
import chess.ChessMove;
import chess.InvalidMoveException;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class WebSocketHandler {

    private final GameService gameService;
    private final Map<Integer, Map<String, Session>> gameSessions = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder()
            .enableComplexMapKeySerialization()
            .create();

    public WebSocketHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("New WebSocket connected: " + session.getRemoteAddress());
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws IOException {
        System.out.println("Received message: " + message);

        UserGameCommand baseCommand = gson.fromJson(message, UserGameCommand.class);
        Integer gameID = baseCommand.getGameID();
        String authToken = baseCommand.getAuthString();

        if (authToken == null || authToken.isEmpty()) {
            sendError(session, "Error: Missing authentication token.");
            return;
        }

        if (gameID == null && baseCommand.getCommandType() != UserGameCommand.CommandType.LEAVE &&
                baseCommand.getCommandType() != UserGameCommand.CommandType.RESIGN &&
                baseCommand.getCommandType() != UserGameCommand.CommandType.MAKE_MOVE) {
            sendError(session, "Error: Missing game ID for this command.");
            return;
        }

        try {
            switch (baseCommand.getCommandType()) {
                case CONNECT:
                    String playerColor = baseCommand.getPlayerColor();
                    if (gameID == null) {
                        throw new DataAccessException("Error: Missing game ID for CONNECT command.");
                    }

                    if (playerColor == null || playerColor.isEmpty()) {
                        gameService.joinGame(authToken, gameID, "observer");
                        gameSessions.computeIfAbsent(gameID, k -> new ConcurrentHashMap<>()).put(authToken, session);
                        sendLoadGame(session, gameID, authToken);
                        sendNotificationToGame(gameID, authToken,
                                gameService.getUsernameFromAuth(authToken) + " joined as observer.");
                    } else {
                        if (!"white".equalsIgnoreCase(playerColor) && !"black".equalsIgnoreCase(playerColor)) {
                            throw new DataAccessException("Error: Invalid player color. Must be 'white', 'black', or null/empty for observer.");
                        }
                        gameService.joinGame(authToken, gameID, playerColor);
                        gameSessions.computeIfAbsent(gameID, k -> new ConcurrentHashMap<>()).put(authToken, session);
                        sendLoadGame(session, gameID, authToken);
                        sendNotificationToGame(gameID, authToken,
                                gameService.getUsernameFromAuth(authToken) + " joined as " + playerColor + " player.");
                    }
                    break;

                case MAKE_MOVE:
                    MakeMoveCommand makeMoveCommand = gson.fromJson(message, MakeMoveCommand.class);
                    ChessMove move = makeMoveCommand.getMove();
                    if (move == null) {
                        throw new DataAccessException("Error: ChessMove is required for MAKE_MOVE.");
                    }
                    gameService.makeMove(gameID, authToken, move);

                    sendLoadGameToAllInGame(gameID);
                    sendNotificationToGame(gameID, null,
                            gameService.getUsernameFromAuth(authToken) + " made a move: " + move.toString() + ".");
                    break;

                case RESIGN:
                    gameService.resign(gameID, authToken);
                    sendNotificationToGame(gameID, null,
                            gameService.getUsernameFromAuth(authToken) + " resigned. Game is over.");
                    break;

                case LEAVE:
                    sendNotificationToGame(gameID, authToken,
                            gameService.getUsernameFromAuth(authToken) + " left the game.");

                    gameService.leaveGame(gameID, authToken);

                    Map<String, Session> sessionsInGame = gameSessions.get(gameID);
                    if (sessionsInGame != null) {
                        sessionsInGame.remove(authToken);
                        if (sessionsInGame.isEmpty()) {
                            gameSessions.remove(gameID);
                        }
                    }
                    break;

                default:
                    sendError(session, "Error: Unknown or unsupported command type.");
                    break;
            }
        } catch (DataAccessException | InvalidMoveException e) {
            System.out.println("Error processing WebSocket command: " + e.getMessage());
            sendError(session, "Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error processing WebSocket command: " + e.getMessage());
            e.printStackTrace();
            sendError(session, "Error: An unexpected server error occurred: " + e.getMessage());
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("WebSocket closed for " + session.getRemoteAddress() + ", Reason: " + reason + ", Status: " + statusCode);
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket error for " + session.getRemoteAddress() + ": " + error.getMessage());
        error.printStackTrace();
        try {
            sendError(session, "Error: An unexpected server error occurred: " + error.getMessage());
        } catch (IOException e) {
            System.err.println("Failed to send error message during onError: " + e.getMessage());
        }
    }

    private void sendError(Session session, String message) throws IOException {
        ServerMessageError errorMessage = new ServerMessageError(message);
        session.getRemote().sendString(gson.toJson(errorMessage));
    }

    private void sendLoadGame(Session session, int gameID, String authToken) throws DataAccessException, IOException {
        ChessGame currentGameState = gameService.getGameState(gameID, authToken);
        LoadGameMessage loadGameMessage = new LoadGameMessage(currentGameState);
        session.getRemote().sendString(gson.toJson(loadGameMessage));
    }

    private void sendLoadGameToAllInGame(int gameID) throws DataAccessException, IOException {
        Map<String, Session> sessions = gameSessions.get(gameID);
        if (sessions != null) {
            for (Map.Entry<String, Session> entry : sessions.entrySet()) {
                Session session = entry.getValue();
                String authToken = entry.getKey();
                if (session.isOpen()) {
                    sendLoadGame(session, gameID, authToken);
                }
            }
        }
    }

    private void sendNotificationToGame(int gameID, String excludeAuthToken, String message) throws IOException {
        Map<String, Session> sessions = gameSessions.get(gameID);
        if (sessions != null) {
            ServerMessageNotification notification = new ServerMessageNotification(message);
            String notificationJson = gson.toJson(notification);
            for (Map.Entry<String, Session> entry : sessions.entrySet()) {
                String authToken = entry.getKey();
                Session session = entry.getValue();
                if (session.isOpen() && !Objects.equals(authToken, excludeAuthToken)) {
                    session.getRemote().sendString(notificationJson);
                }
            }
        }
    }
}