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
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError; // Importar
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
    // Map para almacenar sesiones activas por authToken
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    // Map para almacenar sesiones de juegos por gameID y authToken. Facilita el broadcast.
    private final Map<Integer, Map<String, Session>> gameSessions = new ConcurrentHashMap<>();
    // Map para mapear sesiones de Jetty a authTokens (para onClose)
    private final Map<Session, String> sessionAuthTokens = new ConcurrentHashMap<>();
    // Map para mapear authTokens a gameIDs para rápido acceso
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
        String authToken = sessionAuthTokens.remove(session); // Eliminar del mapeo de sesión a authToken
        System.out.println("WebSocket closed: StatusCode=" + statusCode + ", Reason=" + reason + " for session: " + session.getRemoteAddress());

        if (authToken != null) {
            Integer gameID = authTokenGameIds.remove(authToken); // Eliminar del mapeo de authToken a gameID

            if (gameID != null) {
                Map<String, Session> gameSessionMap = gameSessions.get(gameID);
                if (gameSessionMap != null) {
                    gameSessionMap.remove(authToken); // Eliminar del mapa de sesiones del juego
                    if (gameSessionMap.isEmpty()) {
                        gameSessions.remove(gameID); // Eliminar la entrada del juego si no quedan sesiones
                    }
                }
            }
            sessions.remove(authToken); // Eliminar de la lista principal de sesiones activas

            try {
                String leavingUsername = gameService.getUsernameFromAuth(authToken);
                // Notificar a los demás clientes en el juego que alguien se desconectó
                if (gameID != null) {
                    broadcastNotification(gameID, leavingUsername + " se ha desconectado.", null); // No excluir a nadie, ya se desconectó
                }
            } catch (DataAccessException e) {
                System.err.println("Error al obtener el nombre de usuario para el token desconectado: " + e.getMessage());
            }
        }
    }

    @OnWebSocketMessage
    public void OnMessage(Session session, String message) throws IOException {
        try {
            UserGameCommand baseCommand = gson.fromJson(message, UserGameCommand.class);

            String authToken = baseCommand.getAuthString();
            Integer gameID = baseCommand.getGameID();

            if (authToken == null) {
                sendError(session, "Error: AuthToken faltante en el comando.");
                session.close(4000, "AuthToken faltante.");
                return;
            }

            // Aquí se añade o actualiza la sesión principal y el mapeo de authToken a session.
            sessions.put(authToken, session);
            sessionAuthTokens.put(session, authToken);

            switch (baseCommand.getCommandType()) {
                case CONNECT:
                    ConnectCommand connectCommand = gson.fromJson(message, ConnectCommand.class);
                    gameID = connectCommand.getGameID(); // Asegurarse de tomar el gameID del comando CONNECT
                    ChessGame.TeamColor playerColor = connectCommand.getPlayerColor();

                    if (gameID == null) {
                        sendError(session, "Error: GameID faltante en el comando CONNECT.");
                        session.close(4000, "GameID faltante.");
                        return;
                    }

                    // Asociar el authToken con el gameID para facilitar el seguimiento
                    authTokenGameIds.put(authToken, gameID);

                    String connectingUsername = gameService.getUsernameFromAuth(authToken);

                    // Añadir la sesión al mapa de sesiones del juego
                    gameSessions.computeIfAbsent(gameID, k -> new ConcurrentHashMap<>()).put(authToken, session);

                    // Cargar el estado del juego y enviarlo solo a la sesión que se conecta
                    ChessGame gameStateOnConnect = gameService.getGameState(gameID, authToken);
                    sendMessage(session, new LoadGameMessage(gameStateOnConnect));

                    String playerType = (playerColor != null) ? playerColor.toString().toLowerCase() : "observador";
                    broadcastNotification(gameID, connectingUsername + " se unió al juego " + gameID + " como " + playerType + ".", session);
                    System.out.println(connectingUsername + " conectado al juego " + gameID + " como " + playerType + ".");
                    break;

                case MAKE_MOVE:
                    MakeMoveCommand makeMoveCommand = gson.fromJson(message, MakeMoveCommand.class);
                    // gameID y authToken ya obtenidos del baseCommand

                    // Validar que el gameID del comando coincida con el que tenemos asociado a ese authToken
                    if (!Objects.equals(authTokenGameIds.get(authToken), gameID)) {
                        sendError(session, "Error: GameID del comando no coincide con la sesión actual.");
                        // No cerramos la sesión aquí, solo un error para el cliente
                        return;
                    }

                    String movingUsername = gameService.getUsernameFromAuth(authToken);
                    ChessMove move = makeMoveCommand.getMove();

                    try {
                        gameService.makeMove(gameID, authToken, move);
                        broadcastLoadGame(gameID, authToken); // Cargar el juego actualizado para todos
                        broadcastNotification(gameID, movingUsername + " hizo un movimiento: " + formatMove(move) + ".", null); // A todos

                        // Verificar jaque, jaque mate, tablas
                        ChessGame updatedGame = gameService.getGameState(gameID, authToken);
                        if (updatedGame.isInCheckmate(updatedGame.getTeamTurn())) {
                            broadcastNotification(gameID, updatedGame.getTeamTurn() + " está en jaque mate. ¡La partida ha terminado!", null);
                        } else if (updatedGame.isInStalemate(updatedGame.getTeamTurn())) {
                            broadcastNotification(gameID, updatedGame.getTeamTurn() + " está en tablas por ahogado. ¡La partida ha terminado!", null);
                        } else if (updatedGame.isInCheck(updatedGame.getTeamTurn())) {
                            broadcastNotification(gameID, updatedGame.getTeamTurn() + " está en jaque.", null);
                        }


                        System.out.println(movingUsername + " hizo un movimiento en el juego " + gameID + ".");
                    } catch (InvalidMoveException e) {
                        sendError(session, "Movimiento inválido: " + e.getMessage());
                        System.err.println("InvalidMoveException para " + movingUsername + " en el juego " + gameID + ": " + e.getMessage());
                    } catch (DataAccessException e) {
                        sendError(session, "Error al procesar el movimiento (datos): " + e.getMessage());
                        System.err.println("DataAccessException para " + movingUsername + " en el juego " + gameID + ": " + e.getMessage());
                    }
                    break;

                case RESIGN:
                    // gameID y authToken ya obtenidos del baseCommand
                    if (!Objects.equals(authTokenGameIds.get(authToken), gameID)) {
                        sendError(session, "Error: GameID del comando no coincide con la sesión actual.");
                        return;
                    }

                    String resigningUsername = gameService.getUsernameFromAuth(authToken);
                    try {
                        gameService.resign(gameID, authToken);
                        // El juego termina, notificar a todos
                        broadcastNotification(gameID, resigningUsername + " ha renunciado al juego " + gameID + ". ¡La partida ha terminado!", null);
                        broadcastLoadGame(gameID, authToken); // Enviar el estado final del juego a todos (con isGameOver = true)
                        System.out.println(resigningUsername + " renunció al juego " + gameID + ".");
                    } catch (DataAccessException e) {
                        sendError(session, "La renuncia falló: " + e.getMessage());
                        System.err.println("Resign DataAccessException para " + resigningUsername + " en el juego " + gameID + ": " + e.getMessage());
                    }
                    break;

                case LEAVE:
                    // gameID y authToken ya obtenidos del baseCommand
                    if (!Objects.equals(authTokenGameIds.get(authToken), gameID)) {
                        sendError(session, "Error: GameID del comando no coincide con la sesión actual.");
                        return;
                    }

                    String leavingUsername = gameService.getUsernameFromAuth(authToken);
                    try {
                        gameService.leaveGame(gameID, authToken);

                        // Eliminar la sesión del mapa de sesiones del juego
                        Map<String, Session> gameSessionMap = gameSessions.get(gameID);
                        if (gameSessionMap != null) {
                            gameSessionMap.remove(authToken);
                            if (gameSessionMap.isEmpty()) {
                                gameSessions.remove(gameID); // Eliminar la entrada del juego si no quedan sesiones
                            }
                        }
                        sessionAuthTokens.remove(session); // Eliminar del mapeo de sesión a authToken
                        authTokenGameIds.remove(authToken); // Eliminar del mapeo de authToken a gameID
                        sessions.remove(authToken); // Eliminar de la lista principal de sesiones

                        broadcastNotification(gameID, leavingUsername + " abandonó el juego " + gameID + ".", session); // Notificar a los demás
                        System.out.println(leavingUsername + " abandonó el juego " + gameID + ".");
                    } catch (DataAccessException e) {
                        sendError(session, "El abandono falló: " + e.getMessage());
                        System.err.println("Leave DataAccessException para " + leavingUsername + " en el juego " + gameID + ": " + e.getMessage());
                    }
                    break;

                default:
                    sendError(session, "Error: Tipo de comando desconocido: " + baseCommand.getCommandType());
                    break;
            }
        } catch (DataAccessException e) {
            sendError(session, "Error de autenticación o datos: " + e.getMessage());
            System.err.println("DataAccessException en OnMessage: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) { // Capturar cualquier otra excepción
            sendError(session, "Error interno del servidor inesperado: " + e.getMessage());
            System.err.println("Excepción inesperada en OnMessage: " + e.getMessage());
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
            ServerMessageNotification notification = new ServerMessageNotification(message); // El constructor ahora solo requiere el String 'message'            String notificationJson = gson.toJson(notification);

            for (Session session : gameSessionMap.values()) {
                if (session != excludedSession && session.isOpen()) {
                    try {
                        String notificationJson = "";
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
            ChessGame gameState = gameService.getGameState(gameID, authTokenForServiceCall); // Obtener el estado más reciente

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

    // Helper para formatear un movimiento para la notificación
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


    public void start(int port) {
        Spark.port(port);
        // Asegúrate de que tu `Main` o clase de inicio del servidor
        // pase una instancia de `GameService` a `WebSocketServer`.
        // Ejemplo: spark.Spark.webSocket("/ws", () -> new WebSocketServer(yourGameServiceInstance));
        Spark.webSocket("/ws", WebSocketServer.class); // Esto asume que tienes un constructor sin argumentos o que Spark lo inyecta
        Spark.init();
        System.out.println("Servidor WebSocket iniciado en el puerto " + port);
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
        sessions.clear();
        gameSessions.clear();
        sessionAuthTokens.clear();
        authTokenGameIds.clear();
        System.out.println("Servidor WebSocket detenido.");
    }
}