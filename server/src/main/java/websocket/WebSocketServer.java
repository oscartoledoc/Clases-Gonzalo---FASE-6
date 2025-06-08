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
import dataaccess.DataAccessException; // <--- Importa tu DataAccessException
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import chess.ChessGame;
import java.util.Objects;

@WebSocket
public class WebSocketServer {
    private final GameService gameService;
    private final Map<String, Session> sessions = new HashMap<>();
    private final Map<Integer, Map<String, Session>> gameSessions = new HashMap<>();
    private final Map<Session, String> sessionAuthTokens = new HashMap<>(); // Nuevo mapa

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
            sessions.remove(authToken);
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

            // Si el comando es CONNECT, procesa AuthToken y GameID
            if (command.getCommandType() == UserGameCommand.CommandType.CONNECT) {
                String authToken = command.getAuthString();
                Integer gameID = command.getGameID();

                if (authToken == null || gameID == null) {
                    sendError(session, "Error: AuthToken or GameID missing in CONNECT command.");
                    session.close(4000, "Missing credentials in CONNECT command.");
                    return;
                }

                sessions.put(authToken, session);
                gameSessions.computeIfAbsent(gameID, k -> new HashMap<>()).put(authToken, session);
                sessionAuthTokens.put(session, authToken); // Guardar la asociación Sesión -> AuthToken

                // Lógica de conexión inicial, enviar LOAD_GAME
                sendMessage(session, new ServerMessage(ServerMessage.ServerMessageType.LOAD_GAME), authToken, gameID);
                broadcastNotification(gameID, authToken + " joined game", session);

            } else {
                // Para cualquier otro comando, requiere que la sesión ya esté conectada y tenga su authToken y gameID
                String authToken = sessionAuthTokens.get(session); // Obtener el token de la sesión actual
                Integer gameID = getGameIDForAuthToken(authToken); // Necesitas un método para obtener el gameID a partir del AuthToken

                if (authToken == null || gameID == null) {
                    sendError(session, "Error: Not connected to a game or missing authentication for command: " + command.getCommandType());
                    session.close(4001, "Not authenticated or associated with a game.");
                    return;
                }

                switch (command.getCommandType()) {
                    case MAKE_MOVE:
                        // La lógica de makeMove debe ser llamada con el ChessMove del comando
                        ChessMove move = gson.fromJson(message, ChessMove.class); // Esto asume que el mensaje completo incluye el move
                        // Mejor: el UserGameCommand debería tener un campo ChessMove.
                        // UserGameCommand moveCommand = (UserGameCommand) command;
                        // gameService.makeMove(gameID, authToken, moveCommand.getChessMove());

                        // Si tu TestCommand no deserializa bien el ChessMove directamente en UserGameCommand
                        // puedes tener que hacer una clase de comando específica para MAKE_MOVE
                        // o extraerlo con un JsonParser.
                        // Por ahora, asumimos que UserGameCommand ya tiene el move.

                        // Si UserGameCommand NO tiene ChessMove, necesitas extraerlo del JSON:
                        // JsonObject jsonObject = gson.fromJson(message, JsonObject.class);
                        // ChessMove move = gson.fromJson(jsonObject.get("chessMove"), ChessMove.class); // Ajusta el nombre del campo

                        broadcastLoadGame(gameID, authToken); // Notifica a todos el nuevo estado del juego
                        break;
                    case RESIGN:
                        gameService.resign(gameID, authToken);
                        broadcastNotification(gameID, authToken + " resigned.", session);
                        break;
                    case LEAVE:
                        // Eliminar de los mapas de sesiones
                        sessions.remove(authToken);
                        gameSessions.get(gameID).remove(authToken); // Remueve la sesión de ese juego específico
                        sessionAuthTokens.remove(session); // Remueve también de este mapa

                        broadcastNotification(gameID, authToken + " left the game.", session);
                        // Considera cerrar la sesión aquí si el usuario "sale"
                        // session.close(1000, "User left game");
                        break;
                    default:
                        sendError(session, "Error: Unknown command type: " + command.getCommandType());
                        break;
                }
            }
        } catch (Exception e) {
            sendError(session, "Error processing command: " + e.getMessage());
            System.err.println("Exception in OnMessage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Métodos auxiliares (sin cambios drásticos, pero revisar) ---

    private void sendMessage(Session session, ServerMessage message, String authToken, Integer gameID) {
        if (session != null && session.isOpen()) {
            try {
                String fullMessageJson;
                if (message.getServerMessageType() == ServerMessage.ServerMessageType.LOAD_GAME) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("serverMessageType", message.getServerMessageType().toString());
                    // Asegúrate de que gameService.getGameState(gameID, authToken) retorna un objeto ChessGame
                    // que GSON puede serializar, NO una cadena JSON.
                    map.put("game", gameService.getGameState(gameID, authToken));
                    fullMessageJson = gson.toJson(map);
                } else {
                    // Para NOTIFICATION o ERROR, la estructura JSON debe coincidir con tus clases LoadGameMessage, ServerMessageNotification, ServerMessageError en el cliente.
                    // Si ServerMessage solo tiene el tipo, y el mensaje/error va en una subclase,
                    // necesitas construir el objeto correcto aquí.
                    // Ejemplo para NOTIFICATION:
                    // if (message.getServerMessageType() == ServerMessage.ServerMessageType.NOTIFICATION) {
                    //    fullMessageJson = gson.toJson(new ServerMessageNotification("Some notification message")); // Necesitas el mensaje real
                    // }
                    fullMessageJson = gson.toJson(message); // Esto funciona si message ya es la subclase correcta (e.g., ServerMessageNotification)
                }
                session.getRemote().sendString(fullMessageJson);
            } catch (Exception e) {
                System.err.println("Error sending message: " + e.getMessage());
            }
        }
    }

    private void broadcastNotification(Integer gameID, String message, Session excludedSession) {
        Map<String, Session> gameSessionMap = this.gameSessions.get(gameID);
        if (gameSessionMap != null) {
            Map<String, Object> notificationPayload = new HashMap<>();
            notificationPayload.put("serverMessageType", ServerMessage.ServerMessageType.NOTIFICATION.toString());
            notificationPayload.put("message", message); // El mensaje de la notificación
            String notificationJson = gson.toJson(notificationPayload);

            for (Session session : gameSessionMap.values()) {
                // Asegúrate de que la sesión excluida sea correcta y de que la sesión esté abierta.
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

    private void broadcastLoadGame(Integer gameID, String currentAuthToken) {
        Map<String, Session> gameSessionMap = this.gameSessions.get(gameID);
        if (gameSessionMap != null) {
            Object gameState = null; // Inicializar a null
            try {
                gameState = gameService.getGameState(gameID, currentAuthToken); // <--- Aquí es donde se puede lanzar la excepción
            } catch (DataAccessException e) {
                System.err.println("DataAccessException while trying to get game state for broadcast: " + e.getMessage());
                // Podrías enviar un error a cada sesión del juego si no se pudo obtener el estado
                // Aunque si ya se lanzó la excepción, el estado del juego no está disponible
                // Podrías decidir no broadcast si el estado no se puede obtener.
                // O enviar un mensaje de error a cada cliente afectado si es crítico.
                // Por simplicidad, si hay un error al obtener el estado, no broadcast el LOAD_GAME.
                return; // Salir del método si no se puede obtener el estado del juego.
            }

            // Si llegamos aquí, gameState no es null (o es lo que sea que getGameState devuelva en caso de éxito)
            Map<String, Object> loadGamePayload = new HashMap<>();
            loadGamePayload.put("serverMessageType", ServerMessage.ServerMessageType.LOAD_GAME.toString());
            loadGamePayload.put("game", gameState);
            String loadGameJson = gson.toJson(loadGamePayload);

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
                Map<String, String> errorPayload = new HashMap<>();
                errorPayload.put("serverMessageType", ServerMessage.ServerMessageType.ERROR.toString());
                errorPayload.put("errorMessage", errorMessage);
                session.getRemote().sendString(gson.toJson(errorPayload));
            } catch (IOException e) {
                System.err.println("Error sending error message: " + e.getMessage());
            }
        }
    }

    // Ya no se usan directamente para obtener parámetros de la URL.
    // Solo se usarán para buscar gameID a partir de authToken (si es necesario)
    // o simplemente se eliminan si no hay otros usos.
    private String getAuthTokenFromSession(Session session) {
        // Ahora el AuthToken se obtiene de sessionAuthTokens mapa.
        return sessionAuthTokens.get(session);
    }

    private Integer getGameIDFromSession(Session session) {
        // Necesitarías una forma de obtener el gameID dado una Session.
        // Esto podría ser un problema si una sesión no está en gameSessions.
        // La forma más robusta es tener un mapa Session -> GameID.
        String authToken = sessionAuthTokens.get(session);
        return getGameIDForAuthToken(authToken); // Llama al nuevo método
    }

    // Nuevo método para obtener el GameID dado un AuthToken
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
        spark.Spark.webSocket("/connect", WebSocketServer.class); // Ruta debe coincidir con Server.java
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