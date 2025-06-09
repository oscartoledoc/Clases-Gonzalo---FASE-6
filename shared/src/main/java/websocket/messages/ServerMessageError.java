package websocket.messages;

public class ServerMessageError extends ServerMessage {
    public ServerMessageError(String errorMessage) {
        super(ServerMessageType.ERROR);
        this.errorMessage = errorMessage; // Asigna al campo de la clase base
    }
    // No necesitas un getter aquí si el campo ya está en la base y es accesible
    // public String getErrorMessage() { return super.getErrorMessage(); } // Opcional, pero redundante
}