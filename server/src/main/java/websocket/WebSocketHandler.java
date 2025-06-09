package websocket;

import chess.ChessGame;
import chess.ChessMove;
import chess.InvalidMoveException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import websocket.commands.UserGameCommand;
import websocket.commands.ConnectCommand;
import websocket.commands.MakeMoveCommand;
import websocket.messages.ServerMessageError;
import websocket.messages.LoadGameMessage;
import websocket.messages.ServerMessageNotification;
import websocket.messages.ServerMessage;

import dataaccess.DataAccessException;
import service.GameService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class WebSocketHandler {

    private final GameService gameService;
    private final Map<Integer, Map<String, Session>> gameSessions = new ConcurrentHashMap<>();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<Session, String> sessionAuthTokens = new ConcurrentHashMap<>();
    private final Map<String, Integer> authTokenGameIds = new ConcurrentHashMap<>();


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

        try {
            UserGameCommand baseCommand = gson.fromJson(message, UserGameCommand.class);

            String authToken;
            Integer gameID;

            if (baseCommand.getAuthString() == null || baseCommand.getAuthString().isEmpty()) {
                sendError(session, "Error: Missing authentication token in command.");
                return;
            }
            authToken = baseCommand.getAuthString();

            switch (baseCommand.getCommandType()) {
                case CONNECT:
                    ConnectCommand connectCommand = gson.fromJson(message, ConnectCommand.class);
                    gameID = connectCommand.getGameID();

                    if (gameID == null) {
                        sendError(session, "Error: Missing game ID for CONNECT command.");
                        return;
                    }

                    ChessGame.TeamColor playerColor = connectCommand.getPlayerColor();

                    String connectingUsername = gameService.getUsernameFromAuth(authToken);

                    gameService.joinGame(authToken, gameID, (playerColor != null) ? playerColor.toString() : null);

                    sessions.put(authToken, session);
                    gameSessions.computeIfAbsent(gameID, k -> new ConcurrentHashMap<>()).put(authToken, session);
                    sessionAuthTokens.put(session, authToken);
                    authTokenGameIds.put(authToken, gameID);

                    ChessGame gameStateOnConnect = gameService.getGameState(gameID, authToken);
                    sendMessage(session, new LoadGameMessage(gameStateOnConnect));

                    String playerType = (playerColor != null) ? playerColor.toString() : "observer";
                    sendNotificationToGame(gameID, authToken,
                            connectingUsername + " joined game as " + playerType + ".");
                    break;

                case MAKE_MOVE:
                    MakeMoveCommand makeMoveCommand = gson.fromJson(message, MakeMoveCommand.class);
                    gameID = makeMoveCommand.getGameID();
                    ChessMove move = makeMoveCommand.getMove();

                    if (gameID == null || move == null) {
                        sendError(session, "Error: Game ID or ChessMove is missing for MAKE_MOVE command.");
                        return;
                    }

                    String movingUsername = gameService.getUsernameFromAuth(authToken);
                    gameService.makeMove(gameID, authToken, move);

                    sendLoadGameToAllInGame(gameID, authToken);
                    // LÍNEA ELIMINADA: sendNotificationToGame(gameID, null, movingUsername + " made a move: " + move.toString() + ".");
                    break;

                case RESIGN:
                    gameID = baseCommand.getGameID();

                    if (gameID == null) {
                        sendError(session, "Error: Game ID is missing for RESIGN command.");
                        return;
                    }

                    String resigningUsername = gameService.getUsernameFromAuth(authToken);
                    gameService.resign(gameID, authToken);

                    sendNotificationToGame(gameID, null,
                            resigningUsername + " resigned. Game is over.");
                    sendLoadGameToAllInGame(gameID, authToken);
                    break;

                case LEAVE:
                    gameID = baseCommand.getGameID();

                    if (gameID == null) {
                        sendError(session, "Error: Game ID is missing for LEAVE command.");
                        return;
                    }

                    String leavingUsername = gameService.getUsernameFromAuth(authToken);
                    gameService.leaveGame(gameID, authToken);

                    sessionAuthTokens.remove(session);
                    authTokenGameIds.remove(authToken);
                    sessions.remove(authToken);

                    Map<String, Session> sessionsInGame = gameSessions.get(gameID);
                    if (sessionsInGame != null) {
                        sessionsInGame.remove(authToken);
                        if (sessionsInGame.isEmpty()) {
                            gameSessions.remove(gameID);
                        }
                    }

                    sendNotificationToGame(gameID, authToken,
                            leavingUsername + " left the game.");
                    break;

                default:
                    sendError(session, "Error: Unknown or unsupported command type: " + baseCommand.getCommandType());
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

        String authToken = sessionAuthTokens.remove(session);

        if (authToken != null) {
            Integer gameID = authTokenGameIds.remove(authToken);

            if (gameID != null) {
                Map<String, Session> gameSessionMap = gameSessions.get(gameID);
                if (gameSessionMap != null) {
                    gameSessionMap.remove(authToken);
                    if (gameSessionMap.isEmpty()) {
                        gameSessions.remove(gameID);
                    }
                }
            }
            sessions.remove(authToken);

            System.out.println("WebSocket closed for AuthToken: " + authToken + ", Reason: " + reason);
        } else {
            System.out.println("WebSocket closed for unknown session, Reason: " + reason);
        }
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

    private void sendMessage(Session session, ServerMessage message) throws IOException {
        if (session != null && session.isOpen()) {
            session.getRemote().sendString(gson.toJson(message));
        }
    }

    private void sendLoadGameToAllInGame(int gameID, String authTokenForServiceCall) throws DataAccessException, IOException {
        Map<String, Session> sessionsInGame = gameSessions.get(gameID);
        if (sessionsInGame != null) {
            ChessGame currentGameState = gameService.getGameState(gameID, authTokenForServiceCall);
            LoadGameMessage loadGameMessage = new LoadGameMessage(currentGameState);
            String loadGameJson = gson.toJson(loadGameMessage);

            for (Map.Entry<String, Session> entry : sessionsInGame.entrySet()) {
                Session session = entry.getValue();
                if (session.isOpen()) {
                    session.getRemote().sendString(loadGameJson);
                }
            }
        }
    }

    private void sendNotificationToGame(int gameID, String excludeAuthToken, String message) throws IOException {
        Map<String, Session> sessionsInGame = gameSessions.get(gameID);
        if (sessionsInGame != null) {
            ServerMessageNotification notification = new ServerMessageNotification(message);
            String notificationJson = gson.toJson(notification);
            for (Map.Entry<String, Session> entry : sessionsInGame.entrySet()) {
                String authToken = entry.getKey();
                Session session = entry.getValue();
                if (session.isOpen() && !Objects.equals(authToken, excludeAuthToken)) {
                    session.getRemote().sendString(notificationJson);
                }
            }
        }
    }
}