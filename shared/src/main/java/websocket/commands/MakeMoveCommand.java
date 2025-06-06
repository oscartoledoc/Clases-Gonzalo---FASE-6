package websocket.commands;

import chess.ChessMove;

public class MakeMoveCommand extends UserGameCommand {
    private final ChessMove move;

    public MakeMoveCommand(String authToken, int gameId, ChessMove move) {
        super(CommandType.MAKE_MOVE, authToken, gameId);
        this.move = move;
    }
    public ChessMove getMove() {
        return move;
    }
}
