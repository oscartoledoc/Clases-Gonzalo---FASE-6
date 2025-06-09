package websocket.commands;

import java.util.Objects;

public class UserGameCommand {
    protected CommandType commandType;
    protected String authToken;
    protected Integer gameID;
    // ELIMINAR: protected String playerColor; // ¡QUITAR ESTA LÍNEA!

    // Constructor base para comandos que no requieren playerColor
    public UserGameCommand(CommandType commandType, String authToken, Integer gameID) {
        this.commandType = commandType;
        this.authToken = authToken;
        this.gameID = gameID;
        // ELIMINAR: this.playerColor = null; // ¡QUITAR ESTA LÍNEA!
    }

    // ELIMINAR ESTE CONSTRUCTOR COMPLETO:
    // public UserGameCommand(CommandType commandType, String authToken, Integer gameID, String playerColor) {
    //     this.commandType = commandType;
    //     this.authToken = authToken;
    //     this.gameID = gameID;
    //     this.playerColor = playerColor;
    // }

    public CommandType getCommandType() {
        return commandType;
    }

    public String getAuthString() {
        return authToken;
    }

    public Integer getGameID() {
        return gameID;
    }

    // ELIMINAR ESTE MÉTODO COMPLETO:
    // public String getPlayerColor() {
    //     return playerColor;
    // }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserGameCommand that = (UserGameCommand) o;
        return commandType == that.commandType &&
                Objects.equals(authToken, that.authToken) &&
                Objects.equals(gameID, that.gameID);
        // ELIMINAR: && Objects.equals(playerColor, that.playerColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandType, authToken, gameID);
        // ELIMINAR: playerColor de aquí
    }

    public enum CommandType {
        CONNECT,
        MAKE_MOVE,
        LEAVE,
        RESIGN
    }
}