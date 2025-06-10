package websocket.commands;

import java.util.Objects;

public class UserGameCommand {
    protected CommandType commandType;
    protected String authToken;
    protected Integer gameID;

    public UserGameCommand(CommandType commandType, String authToken, Integer gameID) {
        this.commandType = commandType;
        this.authToken = authToken;
        this.gameID = gameID;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserGameCommand that = (UserGameCommand) o;
        return commandType == that.commandType &&
                Objects.equals(authToken, that.authToken) &&
                Objects.equals(gameID, that.gameID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandType, authToken, gameID);
    }

    public enum CommandType {
        CONNECT,
        MAKE_MOVE,
        LEAVE,
        RESIGN
    }
}