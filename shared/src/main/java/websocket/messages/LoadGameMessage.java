package websocket.messages;

import chess.ChessGame;

public class LoadGameMessage extends ServerMessage {
    public LoadGameMessage(ChessGame game) {
        super(ServerMessageType.LOAD_GAME);
        this.game = game; // Asigna al campo de la clase base
    }
    // public ChessGame getGame() { return super.getGame(); } // Opcional, pero redundante
}