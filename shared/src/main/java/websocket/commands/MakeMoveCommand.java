package websocket.commands;

import chess.ChessMove;

public class MakeMoveCommand extends UserGameCommand {
    private final ChessMove move;

    public MakeMoveCommand(String authToken, Integer gameID, ChessMove move) { // gameId debería ser Integer
        super(CommandType.MAKE_MOVE, authToken, gameID); // Llama al constructor de la clase base
        this.move = move;
    }

    public ChessMove getMove() {
        return move;
    }
}