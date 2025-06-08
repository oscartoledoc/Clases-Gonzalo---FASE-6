package websocket.commands;

import java.util.Objects;

public class UserGameCommand {
    protected CommandType commandType;
    protected String authToken;
    protected Integer gameID;
    protected String playerColor; // Nuevo campo para diferenciar JOIN_PLAYER de JOIN_OBSERVER

    // Constructor para comandos sin playerColor (como MAKE_MOVE, LEAVE, RESIGN)
    public UserGameCommand(String authToken, CommandType commandType, Integer gameID) {
        this.authToken = authToken;
        this.commandType = commandType;
        this.gameID = gameID;
        this.playerColor = null; // Inicialmente nulo
    }

    // Constructor para comandos de conexión que puedan tener playerColor
    public UserGameCommand(String authToken, CommandType commandType, Integer gameID, String playerColor) {
        this.authToken = authToken;
        this.commandType = commandType;
        this.gameID = gameID;
        this.playerColor = playerColor;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public String getAuthString() {
        return authToken;
    }

    public Integer getGameID() {
        return gameID;
    }

    public String getPlayerColor() {
        return playerColor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserGameCommand that = (UserGameCommand) o;
        return commandType == that.commandType &&
                Objects.equals(authToken, that.authToken) &&
                Objects.equals(gameID, that.gameID) &&
                Objects.equals(playerColor, that.playerColor); // Incluye playerColor en equals
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandType, authToken, gameID, playerColor); // Incluye playerColor en hashCode
    }

    public enum CommandType {
        CONNECT, // De vuelta a CONNECT
        MAKE_MOVE,
        LEAVE,
        RESIGN
    }
}