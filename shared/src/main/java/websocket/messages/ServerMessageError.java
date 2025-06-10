package websocket.messages;

public class ServerMessageError extends ServerMessage {
    public ServerMessageError(String errorMessage) {
        super(ServerMessageType.ERROR);
        this.errorMessage = errorMessage;
    }
   }