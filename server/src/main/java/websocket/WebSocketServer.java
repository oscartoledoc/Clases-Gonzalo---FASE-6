package websocket;

import chess.ChessMove;
import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import service.GameService;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;
import dataaccess.DataAccessException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import chess.ChessGame;
import java.util.Objects;
import websocket.messages.LoadGameMessage; // Importa LoadGameMessage
import websocket.messages.ServerMessageNotification; // Importa ServerMessageNotification
import websocket.messages.ServerMessageError; // Importa ServerMessageError

@WebSocket
public class WebSocketServer {
    private final GameService gameService;
    private final Map<String, Session> sessions = new HashMap<>();
    private final Map<Integer, Map<String, Session>> gameSessions = new HashMap<>();
    private final Map<Session, String> sessionAuthTokens = new HashMap<>();

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
            // Remove from sessions map (if used)
            sessions.remove(authToken);
            // Remove from all game sessions
            gameSessions.values().forEach(map -> map.remove(authToken));
            System.out.println("WebSocket closed for AuthToken: " + authToken + ", Reason: " + reason);
        } else {
            System.out.println("WebSocket closed for unknown session, Reason: " + reason);
        }
    }

    @OnWebSocketMessage
    public void OnMessage(Session session, String message) throws IOException {
        try {
            UserGameCommand command = gson.fromJson(message, UserGameCommand.class);

            if (command.getCommandType() == UserGameCommand.CommandType.CONNECT) {
                String authToken = command.getAuthString();
                Integer gameID = command.getGameID();

                if (authToken == null || gameID == null) {
                    sendError(session, "Error: AuthToken or GameID missing in CONNECT command.");
                    session.close(4000, "Missing credentials in CONNECT command.");
                    return;
                }

                // Validar el AuthToken antes de proceder
                gameService.getUsernameFromAuth(authToken); // Esto lanzará DataAccessException si es inválido

                sessions.put(authToken, session);
                gameSessions.computeIfAbsent(gameID, k -> new HashMap<>()).put(authToken, session);
                sessionAuthTokens.put(session, authToken);

                // El mensaje de carga de juego se enviará a través de sendMessage, que ahora puede lanzar DataAccessException
                sendMessage(session, new LoadGameMessage(gameService.getGameState(gameID, authToken)), authToken, gameID);
                broadcastNotification(gameID, gameService.getUsernameFromAuth(authToken) + " joined game as " + command.getPlayerColor(), session);

            } else {
                String authToken = sessionAuthTokens.get(session);
                Integer gameID = getGameIDForAuthToken(authToken);

                if (authToken == null || gameID == null) {
                    sendError(session, "Error: Not connected to a game or missing authentication for command: " + command.getCommandType());
                    session.close(4001, "Not authenticated or associated with a game.");
                    return;
                }

                // Asegúrate de que el usuario está autorizado para el comando (ej: es un jugador o observador)
                // Dependiendo del comando, podrías necesitar más validaciones aquí.
                gameService.getUsernameFromAuth(authToken); // Validar authToken para cualquier comando
                gameService.getGameState(gameID, authToken); // Validar que el juego existe y el usuario tiene acceso

                switch (command.getCommandType()) {
                    case MAKE_MOVE:
                        // La deserialización del move debe hacerse dentro de la clase de comando específica si es necesario.
                        // Para este ejemplo, asumo que ChessMove está directamente en UserGameCommand o se puede obtener.
                        // Si UserGameCommand es solo un wrapper, necesitarás una clase específica para MAKE_MOVE.
                        // Asumiendo que UserGameCommand tiene un campo 'move':
                        // MakeMoveCommand makeMoveCommand = gson.fromJson(message, MakeMoveCommand.class);
                        // gameService.makeMove(gameID, authToken, makeMoveCommand.getMove());
                        break;
                    case RESIGN:
                        gameService.resign(gameID, authToken);
                        broadcastNotification(gameID, gameService.getUsernameFromAuth(authToken) + " resigned.", session);
                        broadcastLoadGame(gameID, authToken); // El juego cambió de estado (terminado)
                        break;
                    case LEAVE:
                        String leavingUsername = gameService.getUsernameFromAuth(authToken);
                        gameService.leaveGame(gameID, authToken); // Llama al método del servicio para manejar la lógica de abandono

                        // Eliminar de los mapas de sesiones (solo si la lógica de servicio lo permite)
                        sessions.remove(authToken);
                        Map<String, Session> currentUsersInGame = gameSessions.get(gameID);
                        if (currentUsersInGame != null) {
                            currentUsersInGame.remove(authToken);
                            if (currentUsersInGame.isEmpty()) {
                                gameSessions.remove(gameID); // Si no quedan usuarios en el juego, elimina la entrada del juego
                            }
                        }
                        sessionAuthTokens.remove(session);

                        broadcastNotification(gameID, leavingUsername + " left the game.", session);
                        // No cierres la sesión aquí a menos que sea un requisito específico,
                        // el cliente debería manejar el cierre después de recibir la notificación.
                        break;
                    default:
                        sendError(session, "Error: Unknown command type: " + command.getCommandType());
                        break;
                }
            }
        } catch (DataAccessException e) {
            // ¡Captura DataAccessException aquí y envía un mensaje de ERROR al cliente!
            sendError(session, "Error: " + e.getMessage());
            System.err.println("DataAccessException in OnMessage: " + e.getMessage());
            e.printStackTrace(); // Para depuración
        } catch (Exception e) {
            // Captura cualquier otra excepción inesperada
            sendError(session, "Error interno del servidor: " + e.getMessage());
            System.err.println("Unexpected Exception in OnMessage: " + e.getMessage());
            e.printStackTrace(); // Para depuración
        }
    }

    // --- Métodos auxiliares ---

    // Modificado para que no capture DataAccessException internamente
    private void sendMessage(Session session, ServerMessage message, String authToken, Integer gameID) throws IOException, DataAccessException {
        if (session != null && session.isOpen()) {
            String fullMessageJson;
            if (message.getServerMessageType() == ServerMessage.ServerMessageType.LOAD_GAME) {
                // Asegúrate de que LoadGameMessage ya tiene el objeto ChessGame
                // o pasas ChessGame directamente aquí.
                // Ya que LoadGameMessage(ChessGame game) lo construye, aquí solo serializamos.
                fullMessageJson = gson.toJson(message); // message es ya una instancia de LoadGameMessage
            } else {
                fullMessageJson = gson.toJson(message);
            }
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
                        System.err.println("Error broadcasting notification: " + e.getMessage());
                    }
                }
            }
        }
    }

    // Modificado para que no capture DataAccessException internamente, y para usar LoadGameMessage
    private void broadcastLoadGame(Integer gameID, String currentAuthToken) throws DataAccessException {
        Map<String, Session> gameSessionMap = this.gameSessions.get(gameID);
        if (gameSessionMap != null) {
            ChessGame gameState = gameService.getGameState(gameID, currentAuthToken); // Esto puede lanzar DataAccessException

            LoadGameMessage loadGame = new LoadGameMessage(gameState);
            String loadGameJson = gson.toJson(loadGame);

            for (Session session : gameSessionMap.values()) {
                if (session.isOpen()) {
                    try {
                        session.getRemote().sendString(loadGameJson);
                    } catch (IOException e) {
                        System.err.println("Error broadcasting LOAD_GAME: " + e.getMessage());
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
                System.err.println("Error sending error message: " + e.getMessage());
            }
        }
    }

    private String getAuthTokenFromSession(Session session) {
        return sessionAuthTokens.get(session);
    }

    private Integer getGameIDFromSession(Session session) {
        String authToken = sessionAuthTokens.get(session);
        return getGameIDForAuthToken(authToken);
    }

    private Integer getGameIDForAuthToken(String authToken) {
        for (Map.Entry<Integer, Map<String, Session>> entry : gameSessions.entrySet()) {
            if (entry.getValue().containsKey(authToken)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void start(int port) {
        spark.Spark.port(port);
        spark.Spark.webSocket("/ws", WebSocketServer.class); // Asegúrate de que la ruta coincida con tu Server.java
        spark.Spark.init();
    }

    public void stop() {
        spark.Spark.stop();
        spark.Spark.awaitStop();
        sessions.clear();
        gameSessions.clear();
        sessionAuthTokens.clear();
        System.out.println("WebSocket server stopped.");
    }
}