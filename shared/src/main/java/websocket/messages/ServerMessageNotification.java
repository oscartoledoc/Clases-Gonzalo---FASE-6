package websocket.messages;

// ServerMessageNotification debe extender ServerMessage
public class ServerMessageNotification extends ServerMessage {
    // El constructor de ServerMessageNotification solo necesita el mensaje,
    // ya que el tipo de mensaje (NOTIFICATION) se pasa a la superclase.
    public ServerMessageNotification(String message) {
        // Llama al constructor de la superclase ServerMessage que toma ServerMessageType y String.
        super(ServerMessageType.NOTIFICATION, message);
    }
}