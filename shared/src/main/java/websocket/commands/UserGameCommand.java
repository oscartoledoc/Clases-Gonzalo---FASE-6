package websocket.commands;

public class UserGameCommand {
    private String authToken;
    private CommandType commandType;
    private Integer gameID; // Usar Integer para permitir null si no aplica a un juego específico

    public enum CommandType {
        CONNECT,
        MAKE_MOVE,
        LEAVE,
        RESIGN
        // Puedes añadir más comandos si los necesitas
    }

    public UserGameCommand(CommandType commandType, String authToken, Integer gameID) {
        this.commandType = commandType;
        this.authToken = authToken;
        this.gameID = gameID;
    }

    public String getAuthString() {
        return authToken;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public Integer getGameID() {
        return gameID;
    }
}