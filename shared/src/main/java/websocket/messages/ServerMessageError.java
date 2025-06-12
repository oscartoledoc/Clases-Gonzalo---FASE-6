package websocket.messages;

// ServerMessageError debe extender ServerMessage
public class ServerMessageError extends ServerMessage {
    // El constructor de ServerMessageError solo necesita el mensaje de error.
    public ServerMessageError(String errorMessage) {
        // Llama al constructor de la superclase ServerMessage que toma un String (para ERROR).
        super(errorMessage);
    }
}