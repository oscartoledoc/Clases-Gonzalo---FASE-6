package websocket.commands;

import chess.ChessGame; // Necesario para ChessGame.TeamColor

public class ConnectCommand extends UserGameCommand {
    private final ChessGame.TeamColor playerColor; // Usar el tipo correcto de ChessGame

    public ConnectCommand(String authToken, Integer gameID, ChessGame.TeamColor playerColor) {
        super(CommandType.CONNECT, authToken, gameID); // Llama al constructor de la clase base
        this.playerColor = playerColor;
    }

    public ChessGame.TeamColor getPlayerColor() {
        return playerColor;
    }
}