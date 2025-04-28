package chess;

public class Main {

    public static void main(String[] args) {

        ChessBoard board = new ChessBoard();

        ChessPosition pos = new ChessPosition(2, 1);
        ChessPiece PAWN = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);


        board.addPiece(pos, PAWN);

        ChessPiece pieza = board.getPiece(pos);

        System.out.println("Pieza obtenida: " + pieza.getPieceType() + "Color: " + pieza.getTeamColor());

    }

}
