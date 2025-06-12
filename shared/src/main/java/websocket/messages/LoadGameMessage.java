package websocket.messages;

import chess.ChessGame;

// LoadGameMessage debe extender ServerMessage
public class LoadGameMessage extends ServerMessage {
    // El constructor de LoadGameMessage solo necesita el objeto ChessGame.
    public LoadGameMessage(ChessGame game) {
        // Llama al constructor de la superclase ServerMessage que toma un ChessGame (para LOAD_GAME).
        super(game);
    }
}