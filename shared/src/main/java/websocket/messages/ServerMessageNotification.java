package websocket.messages;

public class ServerMessageNotification extends ServerMessage {
    public ServerMessageNotification(String message) {
        super(ServerMessageType.NOTIFICATION);
        this.message = message; // Asigna al campo de la clase base
    }
    // public String getMessage() { return super.getMessage(); } // Opcional, pero redundante
}