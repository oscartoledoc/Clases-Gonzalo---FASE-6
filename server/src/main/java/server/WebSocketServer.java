package server;

import chess.ChessGame;
import chess.ChessMove;
import com.google.gson.Gson;
import dataaccess.DataAccess;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import service.GameService;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebSocket
public class WebSocketServer {
    //private final GameService gameService = new GameService();
    private final Map<String, Session> sessions = new HashMap<>();
    private final Map<Integer, Map<String, Session>> gameSessions = new HashMap<>();
    private final Gson gson = new Gson();

    @OnWebSocketConnect
    public void onConnect(Session session) {
        String authToken = session.getUpgradeRequest().getParameterMap().get("AuthToken").get(0);
        Integer gameID = Integer.parseInt(session.getUpgradeRequest().getParameterMap().get("GameID").get(0));
        sessions.put(authToken, session);
        gameSessions.computeIfAbsent(gameID, k -> new HashMap<>()).put(authToken, session);
        sendMessage(session, new ServerMessage(ServerMessage.ServerMessageType.LOAD_GAME), authToken, gameID);
        broadcastNotification(gameID, authToken + " joined game", session);
    }
    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        String authToken = getAuthTokenFromSession(session);
        sessions.remove(authToken);
        gameSessions.values().forEach(map -> map.remove(authToken));
    }

    @OnWebSocketMessage
    public void OnMessage(Session session, String message) throws IOException {
        UserGameCommand command = gson.fromJson(message, UserGameCommand.class);
        String authToken = getAuthTokenFromSession(session);
        Integer gameID = getGameIDFromSession(session);
        try {
            switch (command.getCommandType()) {
                case CONNECT:
                    break;
                case MAKE_MOVE:
                    ChessMove move = gson.fromJson(message, ChessMove.class);
                    //ChessGame.makeMove(move);
                    broadcastLoadGame(gameID, authToken);
                    break;
                case RESIGN:
                    //gameService.resign(gameID, authToken);
                    broadcastNotification(gameID, authToken + " resigned.", session);
                    break;
                case LEAVE:
                    sessions.remove(authToken);
                    gameSessions.get(gameID).remove(authToken);
                    broadcastNotification(gameID, authToken + " left the game.", session);
            }
        } catch (Exception e){
            sendError(session, "Error Processing " + e.getMessage());
        }
    }


    private void sendMessage(Session session, ServerMessage message, String authToken, Integer gameID) {
        if (session.isOpen()) {
            try {
                if (message.getServerMessageType() == ServerMessage.ServerMessageType.LOAD_GAME) {
                    //String json = gson.toJson(message) + ",\"game\":" + gameService.getGameState(gameID, authToken);
                    //session.getRemote().sendString(json);
                    System.out.println("Yeeii");
                } else {
                    session.getRemote().sendString(gson.toJson(message));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcastNotification(Integer gameID, String message, Session excludedSession) {
        Map<String, Session> gameSession = this.gameSessions.get(gameID);
        if (gameSession != null) {
            for (Session session : gameSession.values()) {
                if (session != excludedSession && session.isOpen()) {
                    sendMessage(session, new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION), null, gameID);
                    try {
                        session.getRemote().sendString(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }
    private void broadcastLoadGame(Integer gameID, String authToken) {
        Map<String, Session> gameSession = this.gameSessions.get(gameID);
        if (gameSession != null) {
            for (Session session : gameSession.values()) {
                sendMessage(session, new ServerMessage(ServerMessage.ServerMessageType.LOAD_GAME), authToken, gameID);
            }
        }
    }

    private void sendError(Session session, String errorMessage) {
        if (session.isOpen()) {
            try {
                session.getRemote().sendString(errorMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getAuthTokenFromSession(Session session) {
        return session.getUpgradeRequest().getParameterMap().get("AuthToken").get(0);
    }

    private Integer getGameIDFromSession(Session session) {
        return Integer.parseInt(session.getUpgradeRequest().getParameterMap().get("GameID").get(0));
    }



}

