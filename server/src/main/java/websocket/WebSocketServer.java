package websocket;

import chess.ChessGame;
import chess.ChessMove;
import chess.InvalidMoveException;
import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import service.GameService;
import websocket.commands.UserGameCommand;
import websocket.commands.ConnectCommand;
import websocket.commands.MakeMoveCommand;
import websocket.messages.ServerMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.ServerMessageNotification;
import websocket.messages.ServerMessageError;
import dataaccess.DataAccessException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@WebSocket
public class WebSocketServer {
    private final GameService gameService;
    private final Map<String, Session> sessions = new HashMap<>();
    private final Map<Integer, Map<String, Session>> gameSessions = new HashMap<>();
    private final Map<Session, String> sessionAuthTokens = new HashMap<>();
    private final Map<String, Integer> authTokenGameIds = new HashMap<>();

    private final Gson gson = new Gson();

    public WebSocketServer(GameService gameService) {
        this.gameService = gameService;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("New WebSocket connected: " + session.getRemoteAddress());
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
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

    @OnWebSocketMessage
    public void OnMessage(Session session, String message) throws IOException {
        try {
            UserGameCommand baseCommand = gson.fromJson(message, UserGameCommand.class);

            String authToken;
            Integer gameID;

            switch (baseCommand.getCommandType()) {
                case CONNECT:
                    ConnectCommand connectCommand = gson.fromJson(message, ConnectCommand.class);
                    authToken = connectCommand.getAuthString();
                    gameID = connectCommand.getGameID();

                    if (authToken == null || gameID == null) {
                        sendError(session, "Error: AuthToken o GameID faltante en el comando CONNECT.");
                        session.close(4000, "Credenciales faltantes.");
                        return;
                    }

                    String connectingUsername = gameService.getUsernameFromAuth(authToken);
                    ChessGame.TeamColor playerColor = connectCommand.getPlayerColor();

                    sessions.put(authToken, session);
                    gameSessions.computeIfAbsent(gameID, k -> new HashMap<>()).put(authToken, session);
                    sessionAuthTokens.put(session, authToken);
                    authTokenGameIds.put(authToken, gameID);

                    ChessGame gameStateOnConnect = gameService.getGameState(gameID, authToken);
                    sendMessage(session, new LoadGameMessage(gameStateOnConnect));

                    String playerType = (playerColor != null) ? playerColor.toString() : "observer";
                    broadcastNotification(gameID, connectingUsername + " joined game as " + playerType + ".", session);
                    break;

                case MAKE_MOVE:
                    MakeMoveCommand makeMoveCommand = gson.fromJson(message, MakeMoveCommand.class);
                    authToken = makeMoveCommand.getAuthString();
                    gameID = makeMoveCommand.getGameID();

                    if (authToken == null || gameID == null || !sessionAuthTokens.containsValue(authToken) || !Objects.equals(authTokenGameIds.get(authToken), gameID)) {
                        sendError(session, "Error: No conectado a un juego o autenticación inválida para el comando MAKE_MOVE.");
                        session.close(4001, "No autenticado o asociado con un juego.");
                        return;
                    }

                    String movingUsername = gameService.getUsernameFromAuth(authToken);
                    ChessMove move = makeMoveCommand.getMove();
                    gameService.makeMove(gameID, authToken, move);

                    broadcastLoadGame(gameID, authToken);

                    broadcastNotification(gameID, movingUsername + " made a move.", session);
                    break;

                case RESIGN:
                    authToken = baseCommand.getAuthString();
                    gameID = baseCommand.getGameID();

                    if (authToken == null || gameID == null || !sessionAuthTokens.containsValue(authToken) || !Objects.equals(authTokenGameIds.get(authToken), gameID)) {
                        sendError(session, "Error: No conectado a un juego o autenticación inválida para el comando RESIGN.");
                        session.close(4001, "No autenticado o asociado con un juego.");
                        return;
                    }
                    String resigningUsername = gameService.getUsernameFromAuth(authToken);
                    gameService.resign(gameID, authToken);
                    broadcastNotification(gameID, resigningUsername + " resigned.", null);
                    break;

                case LEAVE:
                    authToken = baseCommand.getAuthString();
                    gameID = baseCommand.getGameID();

                    if (authToken == null || gameID == null || !sessionAuthTokens.containsValue(authToken) || !Objects.equals(authTokenGameIds.get(authToken), gameID)) {
                        sendError(session, "Error: No conectado a un juego o autenticación inválida para el comando LEAVE.");
                        session.close(4001, "No autenticado o asociado con un juego.");
                        return;
                    }
                    String leavingUsername = gameService.getUsernameFromAuth(authToken);
                    gameService.leaveGame(gameID, authToken);

                    sessionAuthTokens.remove(session);
                    authTokenGameIds.remove(authToken);
                    sessions.remove(authToken);

                    Map<String, Session> currentUsersInGame = gameSessions.get(gameID);
                    if (currentUsersInGame != null) {
                        currentUsersInGame.remove(authToken);
                        if (currentUsersInGame.isEmpty()) {
                            gameSessions.remove(gameID);
                        }
                    }

                    broadcastNotification(gameID, leavingUsername + " left the game.", session);
                    break;

                default:
                    sendError(session, "Error: Tipo de comando desconocido: " + baseCommand.getCommandType());
                    break;
            }
        } catch (DataAccessException e) {
            sendError(session, "Error de autenticación o datos: " + e.getMessage());
            System.err.println("DataAccessException in OnMessage: " + e.getMessage());
            e.printStackTrace();
        } catch (InvalidMoveException e) {
            sendError(session, "Movimiento inválido: " + e.getMessage());
            System.err.println("InvalidMoveException in OnMessage: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            sendError(session, "Error interno del servidor inesperado: " + e.getMessage());
            System.err.println("Unexpected Exception in OnMessage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendMessage(Session session, ServerMessage message) throws IOException {
        if (session != null && session.isOpen()) {
            String fullMessageJson = gson.toJson(message);
            session.getRemote().sendString(fullMessageJson);
        }
    }

    private void broadcastNotification(Integer gameID, String message, Session excludedSession) {
        Map<String, Session> gameSessionMap = this.gameSessions.get(gameID);
        if (gameSessionMap != null) {
            ServerMessageNotification notification = new ServerMessageNotification(message);
            String notificationJson = gson.toJson(notification);

            for (Session session : gameSessionMap.values()) {
                if (session != excludedSession && session.isOpen()) {
                    try {
                        session.getRemote().sendString(notificationJson);
                    } catch (IOException e) {
                        System.err.println("Error al transmitir notificación a " + session.getRemoteAddress() + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    private void broadcastLoadGame(Integer gameID, String authTokenForServiceCall) throws DataAccessException {
        Map<String, Session> gameSessionMap = this.gameSessions.get(gameID);
        if (gameSessionMap != null) {
            ChessGame gameState = gameService.getGameState(gameID, authTokenForServiceCall);

            LoadGameMessage loadGame = new LoadGameMessage(gameState);
            String loadGameJson = gson.toJson(loadGame);

            for (Session session : gameSessionMap.values()) {
                if (session.isOpen()) {
                    try {
                        session.getRemote().sendString(loadGameJson);
                    } catch (IOException e) {
                        System.err.println("Error al transmitir LOAD_GAME a " + session.getRemoteAddress() + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    private void sendError(Session session, String errorMessage) {
        if (session != null && session.isOpen()) {
            try {
                ServerMessageError error = new ServerMessageError(errorMessage);
                session.getRemote().sendString(gson.toJson(error));
            } catch (IOException e) {
                System.err.println("Error al enviar mensaje de error: " + e.getMessage());
            }
        }
    }

    private Integer getGameIDForAuthToken(String authToken) {
        return authTokenGameIds.get(authToken);
    }

    public void start(int port) {
        spark.Spark.port(port);
        spark.Spark.webSocket("/ws", WebSocketServer.class);
        spark.Spark.init();
    }

    public void stop() {
        spark.Spark.stop();
        spark.Spark.awaitStop();
        sessions.clear();
        gameSessions.clear();
        sessionAuthTokens.clear();
        authTokenGameIds.clear();
        System.out.println("WebSocket server stopped.");
    }
}