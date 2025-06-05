package websocket;

import chess.ChessGame;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;

import java.net.URI;

public class WebSocketClient {
    private final Gson gson = new Gson();
    private clientListener listener;
    private WebSocketClient client;
    private WebSocketClientEndpoint endpoint;

    public interface clientListener {
        void onGameUpdate(ChessGame game);

        void onNotification(String message);

        void onError(String errorMessage);
    }

    public WebSocketClient() {
    }

    public void setListener(clientListener listener) {
        this.listener = listener;
    }


    public void connect(String wsUrl, String authToken, String gameId) throws Exception {
        if (client != null && client.isRunning()) {
            disconnect();
        }
        client = new WebSocketClient();
        endpoint = new WebSocketClientEndpoint();
        client.start();
        endpoint.setMessageHandler(message -> {
            handleMessage(message);
        });
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


    public class LoadGameMessage extends ServerMessage {
        private final ChessGame game;
        public LoadGameMessage(ChessGame game) {
            super(ServerMessageType.LOAD_GAME);
            this.game = game;
        }
        public ChessGame getGame() {
            return game;
        }
    }

    public class ServerMessageError extends ServerMessage {
        private final String errorMessage;
        public ServerMessageError(String errorMessage) {
            super(ServerMessageType.ERROR);
            this.errorMessage = errorMessage;
        }
        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public class ServerMessageNotification extends ServerMessage {
        private final String message;
        public ServerMessageNotification(String message) {
            super(ServerMessageType.NOTIFICATION);
            this.message = message;
        }
        public String getMessage() {
            return message;
        }
    }