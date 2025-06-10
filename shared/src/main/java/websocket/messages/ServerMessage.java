package websocket.messages;

import chess.ChessGame;
import java.util.Objects;

/**
 * Represents a Message the server can send through a WebSocket
 *
 * Note: You can add to this class, but you should not alter the existing
 * methods.
 */
public class ServerMessage {
    ServerMessageType serverMessageType;

    protected String errorMessage; // Para mensajes de ERROR
    protected String message;      // Para mensajes de NOTIFICATION
    protected ChessGame game;      // Para mensajes de LOAD_GAME

    public enum ServerMessageType {
        LOAD_GAME,
        ERROR,
        NOTIFICATION
    }

    public ServerMessage(ServerMessageType type) {
        this.serverMessageType = type;
        this.errorMessage = null; // Inicializar a nulo
        this.message = null;      // Inicializar a nulo
        this.game = null;         // Inicializar a nulo
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getMessage() {
        return message;
    }

    public ChessGame getGame() {
        return game;
    }

    public ServerMessageType getServerMessageType() {
        return this.serverMessageType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServerMessage)) {
            return false;
        }
        ServerMessage that = (ServerMessage) o;
        return getServerMessageType() == that.getServerMessageType() &&
                Objects.equals(errorMessage, that.errorMessage) &&
                Objects.equals(message, that.message) &&
                Objects.equals(game, that.game);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getServerMessageType(), errorMessage, message, game);
    }
}