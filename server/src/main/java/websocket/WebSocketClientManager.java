package websocket;

import chess.ChessGame;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import websocket.commands.UserGameCommand;
import websocket.messages.LoadGameMessage;
import websocket.messages.ServerMessage;
import websocket.messages.ServerMessageError;
import websocket.messages.ServerMessageNotification;

import java.net.URI;
import java.util.concurrent.TimeUnit;

public class WebSocketClientManager {
    private final Gson gson = new Gson();
    private clientListener listener;
    private WebSocketClient client;
    private WebSocketClientEndpoint endpoint;
    private boolean isRunning = false;

    // Nuevo: Guardar authToken y gameId para enviarlos en el primer mensaje
    private String currentAuthToken;
    private Integer currentGameID;

    public interface clientListener {
        void onGameUpdate(ChessGame game);
        void onNotification(String message);
        void onError(String errorMessage);
    }

    public WebSocketClientManager() {
        this.client = new WebSocketClient();
        try {
            client.start();
            isRunning = true;
        } catch (Exception e) {
            System.err.println("Error starting WebSocketClient: " + e.getMessage());
            // Manejar error de inicialización
        }
    }

    public void setListener(clientListener listener) {
        this.listener = listener;
    }

    public void connect(String wsUrl, String authToken, String gameId) throws Exception {
        // Almacenar el authToken y gameId.
        this.currentAuthToken = authToken;
        this.currentGameID = Integer.parseInt(gameId); // Asume que gameId siempre es parseable.

        URI uri = new URI(wsUrl); // La URL ya no necesita los parámetros aquí.

        endpoint = new WebSocketClientEndpoint();
        // Configura el manejador de mensajes ANTES de conectar.
        endpoint.setMessageHandler(this::handleMessage);

        ClientUpgradeRequest request = new ClientUpgradeRequest();

        // Conectar al servidor WebSocket
        client.connect(endpoint, uri, request).get(5, TimeUnit.SECONDS);

        // ¡IMPORTANTE! Enviar el comando CONNECT después de establecer la conexión
        // Esto es lo que tu servidor ahora espera como primer mensaje.
        sendCommand(new UserGameCommand(UserGameCommand.CommandType.CONNECT, currentAuthToken, currentGameID));
    }

    public void disconnect() {
        if (client != null && client.isRunning()) {
            try {
                client.stop();
            } catch (Exception e) {
                System.err.println("Error stopping websocket client: " + e.getMessage());
            } finally {
                client = null;
                endpoint = null;
                isRunning = false;
                currentAuthToken = null; // Limpiar datos de la sesión
                currentGameID = null;
            }
        }
    }

    public void sendCommand(UserGameCommand command) {
        if (endpoint != null && endpoint.isOpen()) {
            try {
                endpoint.sendMessage(gson.toJson(command));
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError("Failed to send command: " + e.getMessage());
                }
            }
        } else {
            if (listener != null) {
                listener.onError("WebSocket connection not open. Cannot send command.");
            }
        }
    }

    private void handleMessage(String message) {
        JsonObject json = gson.fromJson(message, JsonObject.class);
        if (!json.has("serverMessageType")) {
            if (listener != null) {
                listener.onError("Received message without 'serverMessageType': " + message);
            }
            return;
        }

        try {
            ServerMessage.ServerMessageType type = ServerMessage.ServerMessageType.valueOf(json.get("serverMessageType").getAsString());
            switch (type) {
                case LOAD_GAME -> {
                    LoadGameMessage loadGameMsg = gson.fromJson(message, LoadGameMessage.class);
                    if (listener != null) {
                        listener.onGameUpdate(loadGameMsg.getGame());
                    }
                }
                case NOTIFICATION -> {
                    ServerMessageNotification notificationmsg = gson.fromJson(message, ServerMessageNotification.class);
                    if (listener != null) {
                        listener.onNotification(notificationmsg.getMessage());
                    }
                }
                case ERROR -> {
                    ServerMessageError errorMsg = gson.fromJson(message, ServerMessageError.class);
                    if (listener != null) {
                        listener.onError(errorMsg.getErrorMessage());
                    }
                }
                default -> {
                    if (listener != null) {
                        listener.onError("Received unknown server message type: " + type + " - " + message);
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            if (listener != null) {
                listener.onError("Unknown server message type string: " + json.get("serverMessageType").getAsString());
            }
        } catch (Exception e) {
            if (listener != null) {
                listener.onError("Error parsing server message: " + e.getMessage() + ". Original message: " + message);
            }
        }
    }
}