package websocket.commands;

import chess.ChessGame;

public class ConnectCommand extends UserGameCommand {
    private ChessGame.TeamColor playerColor; // Puede ser null para observadores

    public ConnectCommand(String authToken, Integer gameID, ChessGame.TeamColor playerColor) {
        super(CommandType.CONNECT, authToken, gameID);
        this.playerColor = playerColor;
    }

    public ChessGame.TeamColor getPlayerColor() {
        return playerColor;
    }
}