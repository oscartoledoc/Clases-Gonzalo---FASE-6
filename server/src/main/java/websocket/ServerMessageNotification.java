package websocket;

import websocket.messages.ServerMessage;

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