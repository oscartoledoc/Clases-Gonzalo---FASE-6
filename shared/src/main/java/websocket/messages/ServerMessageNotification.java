package websocket.messages;

public class ServerMessageNotification extends ServerMessage {
    public ServerMessageNotification(String message) {
        super(ServerMessageType.NOTIFICATION);
        this.message = message;
    }
}