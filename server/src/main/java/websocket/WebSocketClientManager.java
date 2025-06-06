package websocket;

import chess.ChessGame;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

import java.net.URI;

public class WebSocketClientManager {
    private final Gson gson = new Gson();
    private clientListener listener;
    private WebSocketClient client;
    private WebSocketClientEndpoint endpoint;
    private boolean isRunning = false;

    public interface clientListener {
        void onGameUpdate(ChessGame game);
        void onNotification(String message);
        void onError(String errorMessage);
    }

    public void setListener(clientListener listener) {
        this.listener = listener;
    }


    public void connect(String wsUrl, String authToken, String gameId) throws Exception {
        if (client != null && isRunning) {
            disconnect();
        }
        endpoint = new WebSocketClientEndpoint();
        endpoint.setMessageHandler(this::handleMessage);
        URI uri = new URI(wsUrl);
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        client.connect(endpoint, uri, request);
        UserGameCommand connectCommand = new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, Integer.parseInt(gameId));
    }

    public void disconnect() {
        try {
            client.stop();
        } catch (Exception e) {
            System.err.println("Error stopping websocket client: " + e.getMessage());
        }
        client = null;
        endpoint = null;
    }

    public void sendCommand(UserGameCommand command) {
        if (endpoint != null) {
            try {
                endpoint.sendMessage(gson.toJson(command));
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        }
    }

    private void handleMessage(String message) {
        JsonObject json = gson.fromJson(message, JsonObject.class);
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
        }
    }
}