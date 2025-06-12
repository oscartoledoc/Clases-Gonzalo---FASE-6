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

    // Campos para diferentes tipos de mensajes
    protected String errorMessage; // Para mensajes de ERROR
    protected String message;      // Para mensajes de NOTIFICATION
    protected ChessGame game;      // Para mensajes de LOAD_GAME

    public enum ServerMessageType {
        LOAD_GAME,
        ERROR,
        NOTIFICATION
    }

    // Constructor base que solo establece el tipo de mensaje
    public ServerMessage(ServerMessageType type) {
        this.serverMessageType = type;
    }

    // --- Constructores específicos para cada tipo de mensaje ---
    // (Estos constructores son convenientes para CREAR mensajes en el servidor)

    // Constructor para mensajes de ERROR
    public ServerMessage(String errorMessage) {
        this.serverMessageType = ServerMessageType.ERROR;
        this.errorMessage = errorMessage;
    }

    // Constructor para mensajes de NOTIFICATION
    public ServerMessage(ServerMessageType type, String message) { // Cambié el orden para evitar colisiones
        this.serverMessageType = type; // Asumimos que aquí siempre será NOTIFICATION
        this.message = message;
    }

    // Constructor para mensajes de LOAD_GAME
    public ServerMessage(ChessGame game) { // Este constructor es para LOAD_GAME
        this.serverMessageType = ServerMessageType.LOAD_GAME;
        this.game = game;
    }

    // --- Getters para todos los campos ---
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