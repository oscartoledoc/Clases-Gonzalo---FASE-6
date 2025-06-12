package websocket;

import chess.ChessGame;
import chess.ChessMove;
import chess.InvalidMoveException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import service.GameService;
import spark.Spark;
import websocket.commands.UserGameCommand;
import websocket.commands.ConnectCommand;
import websocket.commands.MakeMoveCommand;
import websocket.messages.ServerMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.ServerMessageNotification;
import websocket.messages.ServerMessageError;
import dataaccess.DataAccessException;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class WebSocketServer {
    private final GameService gameService;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<Integer, Map<String, Session>> gameSessions = new ConcurrentHashMap<>();
    private final Map<Session, String> sessionAuthTokens = new ConcurrentHashMap<>();
    private final Map<String, Integer> authTokenGameIds = new ConcurrentHashMap<>();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

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
        System.out.println("WebSocket closed: StatusCode=" + statusCode + ", Reason=" + reason + " for session: " + session.getRemoteAddress());

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

            try {
                String leavingUsername = gameService.getUsernameFromAuth(authToken);
                if (gameID != null) {
                    broadcastNotification(gameID, leavingUsername + " se ha desconectado.", null);
                }
            } catch (DataAccessException e) {
                System.err.println("Error al obtener el nombre de usuario para el token desconectado: " + e.getMessage());
            }
        }
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws IOException {
        try {
            UserGameCommand baseCommand = gson.fromJson(message, UserGameCommand.class);

            String authToken = baseCommand.getAuthString();
            Integer gameID = baseCommand.getGameID();

            if (authToken == null) {
                sendError(session, "Error: AuthToken faltante en el comando.");
                session.close(4000, "AuthToken faltante.");
                return;
            }

            sessions.put(authToken, session);
            sessionAuthTokens.put(session, authToken);

            switch (baseCommand.getCommandType()) {
                case CONNECT:
                    ConnectCommand connectCommand = gson.fromJson(message, ConnectCommand.class);
                    gameID = connectCommand.getGameID();
                    ChessGame.TeamColor playerColor = connectCommand.getPlayerColor();

                    if (gameID == null) {
                        sendError(session, "Error: GameID faltante en el comando CONNECT.");
                        session.close(4000, "GameID faltante.");
                        return;
                    }

                    authTokenGameIds.put(authToken, gameID);

                    gameSessions.computeIfAbsent(gameID, k -> new ConcurrentHashMap<>()).put(authToken, session);

                    ChessGame gameStateOnConnect = gameService.getGameState(gameID, authToken);
                    sendMessage(session, new LoadGameMessage(gameStateOnConnect));

                    String playerType = (playerColor != null) ? playerColor.toString().toLowerCase() : "observador";
                    broadcastNotification(gameID, gameService.getUsernameFromAuth(authToken) + " se unió al juego " + gameID + " como " + playerType + ".", session);
                    System.out.println(gameService.getUsernameFromAuth(authToken) + " conectado al juego " + gameID + " como " + playerType + ".");
                    break;

                case MAKE_MOVE:
                    MakeMoveCommand makeMoveCommand = gson.fromJson(message, MakeMoveCommand.class);

                    if (!Objects.equals(authTokenGameIds.get(authToken), gameID)) {
                        sendError(session, "Error: GameID del comando no coincide con la sesión actual.");
                        return;
                    }

                    String movingUsername = gameService.getUsernameFromAuth(authToken);
                    ChessMove move = makeMoveCommand.getMove();

                    try {
                        gameService.makeMove(gameID, authToken, move);
                        broadcastLoadGame(gameID, authToken);
                        broadcastNotification(gameID, movingUsername + " hizo un movimiento: " + formatMove(move) + ".", null);

                        ChessGame updatedGame = gameService.getGameState(gameID, authToken);
                        if (updatedGame.isInCheckmate(updatedGame.getTeamTurn())) {
                            broadcastNotification(gameID, updatedGame.getTeamTurn() + " está en jaque mate. ¡La partida ha terminado!", null);
                        } else if (updatedGame.isInStalemate(updatedGame.getTeamTurn())) {
                            broadcastNotification(gameID, updatedGame.getTeamTurn() + " está en tablas por ahogado. ¡La partida ha terminado!", null);
                        } else if (updatedGame.isInCheck(updatedGame.getTeamTurn())) {
                            broadcastNotification(gameID, updatedGame.getTeamTurn() + " está en jaque.", null);
                        }
                    } catch (InvalidMoveException e) {
                        sendError(session, "Movimiento inválido: " + e.getMessage());
                    } catch (DataAccessException e) {
                        sendError(session, "Error al procesar el movimiento (datos): " + e.getMessage());
                    }
                    break;

                case RESIGN:
                    if (!Objects.equals(authTokenGameIds.get(authToken), gameID)) {
                        sendError(session, "Error: GameID del comando no coincide con la sesión actual.");
                        return;
                    }

                    String resigningUsername = gameService.getUsernameFromAuth(authToken);
                    try {
                        gameService.resign(gameID, authToken);
                        broadcastNotification(gameID, resigningUsername + " ha renunciado al juego " + gameID + ". ¡La partida ha terminado!", null);
                        broadcastLoadGame(gameID, authToken);
                    } catch (DataAccessException e) {
                        sendError(session, "La renuncia falló: " + e.getMessage());
                    }
                    break;

                case LEAVE:
                    if (!Objects.equals(authTokenGameIds.get(authToken), gameID)) {
                        sendError(session, "Error: GameID del comando no coincide con la sesión actual.");
                        return;
                    }

                    String leavingUsername = gameService.getUsernameFromAuth(authToken);
                    try {
                        gameService.leaveGame(gameID, authToken);

                        Map<String, Session> gameSessionMap = gameSessions.get(gameID);
                        if (gameSessionMap != null) {
                            gameSessionMap.remove(authToken);
                            if (gameSessionMap.isEmpty()) {
                                gameSessions.remove(gameID);
                            }
                        }
                        sessionAuthTokens.remove(session);
                        authTokenGameIds.remove(authToken);
                        sessions.remove(authToken);

                        broadcastNotification(gameID, leavingUsername + " abandonó el juego " + gameID + ".", session);
                    } catch (DataAccessException e) {
                        sendError(session, "El abandono falló: " + e.getMessage());
                    }
                    break;

                default:
                    sendError(session, "Error: Tipo de comando desconocido: " + baseCommand.getCommandType());
                    break;
            }
        } catch (DataAccessException e) {
            sendError(session, "Error de autenticación o datos: " + e.getMessage());
        } catch (Exception e) {
            sendError(session, "Error interno del servidor inesperado: " + e.getMessage());
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket error for " + session.getRemoteAddress() + ": " + error.getMessage());
        error.printStackTrace();
        sendError(session, "Error: An unexpected server error occurred: " + error.getMessage());
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

    private String formatMove(ChessMove move) {
        char startCol = (char) ('a' + move.getStartPosition().getColumn() - 1);
        int startRow = move.getStartPosition().getRow();
        char endCol = (char) ('a' + move.getEndPosition().getColumn() - 1);
        int endRow = move.getEndPosition().getRow();
        String moveStr = String.format("%c%d to %c%d", startCol, startRow, endCol, endRow);
        if (move.getPromotionPiece() != null) {
            moveStr += " (promoción a " + move.getPromotionPiece().toString() + ")";
        }
        return moveStr;
    }

    public void stop() {
        sessions.clear();
        gameSessions.clear();
        sessionAuthTokens.clear();
        authTokenGameIds.clear();
        System.out.println("Estado interno de WebSocketServer detenido.");
    }
}