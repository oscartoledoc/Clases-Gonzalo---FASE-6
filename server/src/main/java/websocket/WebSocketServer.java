package websocket;

import chess.ChessGame;
import chess.ChessMove;
import chess.InvalidMoveException; // ¡IMPORTANTE! Asegúrate de que esta excepción esté importada
import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import service.GameService;
import websocket.commands.UserGameCommand;
import websocket.commands.ConnectCommand; // ¡IMPORTAR!
import websocket.commands.MakeMoveCommand; // ¡IMPORTAR!
import websocket.messages.ServerMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.ServerMessageNotification;
import websocket.messages.ServerMessageError;
import dataaccess.DataAccessException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects; // Necesario si usas Objects.equals, etc.

@WebSocket
public class WebSocketServer {
    private final GameService gameService;
    // 'sessions' podría considerarse redundante si gameSessions ya maneja todas las sesiones activas,
    // pero si lo usas para otros fines (ej. enviar mensajes a un authToken sin saber el gameID), puedes mantenerlo.
    private final Map<String, Session> sessions = new HashMap<>(); // authToken -> Session
    private final Map<Integer, Map<String, Session>> gameSessions = new HashMap<>(); // gameID -> authToken -> Session
    private final Map<Session, String> sessionAuthTokens = new HashMap<>(); // session -> authToken

    // ¡NUEVO MAPA! Este es crucial para la gestión de sesiones al desconectar y al obtener el gameID.
    private final Map<String, Integer> authTokenGameIds = new HashMap<>(); // authToken -> gameID

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
        // 1. Obtener y remover el authToken de la sesión cerrada
        String authToken = sessionAuthTokens.remove(session);

        if (authToken != null) {
            // 2. Obtener y remover el gameID asociado con este authToken para la sesión que se cerró
            Integer gameID = authTokenGameIds.remove(authToken);

            // 3. Remover el authToken de la lista de sesiones activas del juego específico
            if (gameID != null) {
                Map<String, Session> gameSessionMap = gameSessions.get(gameID);
                if (gameSessionMap != null) {
                    gameSessionMap.remove(authToken);
                    // Si el mapa de sesiones para ese juego queda vacío, puedes remover la entrada del juego
                    if (gameSessionMap.isEmpty()) {
                        gameSessions.remove(gameID);
                    }
                }
            }
            // Opcional: Si 'sessions' es un mapa global para todos los tokens, también remuévelo de aquí.
            sessions.remove(authToken);

            System.out.println("WebSocket closed for AuthToken: " + authToken + ", Reason: " + reason);
        } else {
            System.out.println("WebSocket closed for unknown session, Reason: " + reason);
        }
    }

    @OnWebSocketMessage
    public void OnMessage(Session session, String message) throws IOException {
        try {
            // Paso 1: Deserializar a UserGameCommand para determinar el tipo de comando
            UserGameCommand baseCommand = gson.fromJson(message, UserGameCommand.class);

            String authToken;
            Integer gameID;

            switch (baseCommand.getCommandType()) {
                case CONNECT:
                    // Paso 2.1: Deserializar a ConnectCommand para acceder a playerColor
                    ConnectCommand connectCommand = gson.fromJson(message, ConnectCommand.class);
                    authToken = connectCommand.getAuthString();
                    gameID = connectCommand.getGameID();

                    if (authToken == null || gameID == null) {
                        sendError(session, "Error: AuthToken o GameID faltante en el comando CONNECT.");
                        session.close(4000, "Credenciales faltantes.");
                        return;
                    }

                    // Validar el AuthToken y obtener el nombre de usuario
                    String connectingUsername = gameService.getUsernameFromAuth(authToken);
                    ChessGame.TeamColor playerColor = connectCommand.getPlayerColor(); // ¡Obtener playerColor del ConnectCommand!

                    // Añadir/actualizar los mapas de sesiones
                    sessions.put(authToken, session); // Podrías eliminar este si no lo usas
                    gameSessions.computeIfAbsent(gameID, k -> new HashMap<>()).put(authToken, session);
                    sessionAuthTokens.put(session, authToken);
                    authTokenGameIds.put(authToken, gameID); // ¡IMPORTANTE: Registrar en el nuevo mapa!

                    // Envía el estado del juego al cliente que se acaba de conectar
                    ChessGame gameStateOnConnect = gameService.getGameState(gameID, authToken);
                    sendMessage(session, new LoadGameMessage(gameStateOnConnect));

                    // Transmite una notificación a todos los demás clientes en el juego
                    String playerType = (playerColor != null) ? playerColor.toString() : "observer";
                    broadcastNotification(gameID, connectingUsername + " joined game as " + playerType + ".", session);
                    break;

                case MAKE_MOVE:
                    // Paso 2.2: Deserializar a MakeMoveCommand
                    MakeMoveCommand makeMoveCommand = gson.fromJson(message, MakeMoveCommand.class);
                    authToken = makeMoveCommand.getAuthString();
                    gameID = makeMoveCommand.getGameID();

                    // Validar que la sesión está asociada con un juego y el authToken es válido
                    if (authToken == null || gameID == null || !sessionAuthTokens.containsValue(authToken) || !Objects.equals(authTokenGameIds.get(authToken), gameID)) {
                        sendError(session, "Error: No conectado a un juego o autenticación inválida para el comando MAKE_MOVE.");
                        session.close(4001, "No autenticado o asociado con un juego.");
                        return;
                    }

                    // Llama al servicio para realizar el movimiento
                    String movingUsername = gameService.getUsernameFromAuth(authToken); // Validar y obtener el nombre de usuario
                    ChessMove move = makeMoveCommand.getMove();
                    gameService.makeMove(gameID, authToken, move); // Esto puede lanzar InvalidMoveException/DataAccessException

                    // Después de un movimiento exitoso, carga el juego para todos los clientes en la partida
                    broadcastLoadGame(gameID, authToken); // Carga el juego para TODOS, incluyendo el que hizo el movimiento

                    // Notifica a todos sobre el movimiento
                    broadcastNotification(gameID, movingUsername + " made a move.", null); // 'null' para notificar a todos
                    break;

                case RESIGN:
                    authToken = baseCommand.getAuthString();
                    gameID = baseCommand.getGameID(); // Obtener gameID del comando base

                    if (authToken == null || gameID == null || !sessionAuthTokens.containsValue(authToken) || !Objects.equals(authTokenGameIds.get(authToken), gameID)) {
                        sendError(session, "Error: No conectado a un juego o autenticación inválida para el comando RESIGN.");
                        session.close(4001, "No autenticado o asociado con un juego.");
                        return;
                    }
                    String resigningUsername = gameService.getUsernameFromAuth(authToken);
                    gameService.resign(gameID, authToken);
                    broadcastNotification(gameID, resigningUsername + " resigned.", null); // Notificar a todos, incluyendo el que renunció
                    broadcastLoadGame(gameID, authToken); // El juego cambió de estado (terminado)
                    break;

                case LEAVE:
                    authToken = baseCommand.getAuthString();
                    gameID = baseCommand.getGameID(); // Obtener gameID del comando base

                    if (authToken == null || gameID == null || !sessionAuthTokens.containsValue(authToken) || !Objects.equals(authTokenGameIds.get(authToken), gameID)) {
                        sendError(session, "Error: No conectado a un juego o autenticación inválida para el comando LEAVE.");
                        session.close(4001, "No autenticado o asociado con un juego.");
                        return;
                    }
                    String leavingUsername = gameService.getUsernameFromAuth(authToken);
                    gameService.leaveGame(gameID, authToken); // Llama al método del servicio para manejar la lógica de abandono

                    // Limpieza de sesiones para el usuario que abandona
                    sessionAuthTokens.remove(session); // Remueve la sesión actual
                    authTokenGameIds.remove(authToken); // Remueve el token del mapa de juegos
                    sessions.remove(authToken); // Si usas este mapa global

                    Map<String, Session> currentUsersInGame = gameSessions.get(gameID);
                    if (currentUsersInGame != null) {
                        currentUsersInGame.remove(authToken); // Remueve el token de la sesión de juego específica
                        if (currentUsersInGame.isEmpty()) {
                            gameSessions.remove(gameID); // Si no quedan usuarios en el juego, elimina la entrada del juego
                        }
                    }

                    // Notificar a los clientes restantes en el juego
                    broadcastNotification(gameID, leavingUsername + " left the game.", session); // Excluir la sesión que abandona
                    // No cierres la sesión aquí; el cliente o el método onClose la manejarán.
                    break;

                default:
                    sendError(session, "Error: Tipo de comando desconocido: " + baseCommand.getCommandType());
                    break;
            }
        } catch (DataAccessException e) {
            sendError(session, "Error de autenticación o datos: " + e.getMessage());
            System.err.println("DataAccessException in OnMessage: " + e.getMessage());
            e.printStackTrace();
        } catch (InvalidMoveException e) { // ¡Captura la excepción de movimiento inválido!
            sendError(session, "Movimiento inválido: " + e.getMessage());
            System.err.println("InvalidMoveException in OnMessage: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            sendError(session, "Error interno del servidor inesperado: " + e.getMessage());
            System.err.println("Unexpected Exception in OnMessage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Métodos auxiliares ---

    // Envía un mensaje a una única sesión (LOAD_GAME para el cliente que se conecta)
    private void sendMessage(Session session, ServerMessage message) throws IOException {
        if (session != null && session.isOpen()) {
            String fullMessageJson = gson.toJson(message);
            session.getRemote().sendString(fullMessageJson);
        }
    }

    // Envía una notificación a todos los clientes en un juego, excluyendo una sesión si se especifica
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

    // Transmite el estado actual del juego a todos los clientes en una partida
    private void broadcastLoadGame(Integer gameID, String authTokenForServiceCall) throws DataAccessException {
        Map<String, Session> gameSessionMap = this.gameSessions.get(gameID);
        if (gameSessionMap != null) {
            // Obtiene el estado más reciente del juego usando el servicio
            // El authTokenForServiceCall es para que el servicio pueda validar el acceso
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

    // Envía un mensaje de error a una única sesión
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

    // Ahora este método usa el nuevo mapa para una búsqueda directa y eficiente
    private Integer getGameIDForAuthToken(String authToken) {
        return authTokenGameIds.get(authToken);
    }

    public void start(int port) {
        spark.Spark.port(port);
        // Asegúrate de que la ruta coincida con tu Server.java si lo estás usando
        spark.Spark.webSocket("/ws", WebSocketServer.class);
        spark.Spark.init();
    }

    public void stop() {
        spark.Spark.stop();
        spark.Spark.awaitStop();
        // Limpiar todos los mapas al detener el servidor
        sessions.clear();
        gameSessions.clear();
        sessionAuthTokens.clear();
        authTokenGameIds.clear(); // ¡Limpiar también el nuevo mapa!
        System.out.println("WebSocket server stopped.");
    }
}