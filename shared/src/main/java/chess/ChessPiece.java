package chess;

import java.util.Collection;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    private final ChessGame.TeamColor pieceColor;
    private PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        int row = myPosition.getRow();
        int col = myPosition.getColumn();


        switch (type) {
            case PAWN:
                int direction = pieceColor == ChessGame.TeamColor.WHITE ? 1 : -1;
                int start_row = pieceColor == ChessGame.TeamColor.WHITE ? 2 : 7;

            case KING:
                for (int row_offset = 1; row_offset <= 8; row_offset++) {
                    for (int col_offset = 1; col_offset <= 8; col_offset++) {
                        int new_row = row + row_offset;
                        int new_col = col + col_offset;
                    }
                }


            case KNIGHT:
                int [][] knight_moves = {
                        {2, 1},
                        {2, -1},
                        {-2, 1},
                        {-2, -1},
                        {1, 2},
                        {1, -2},
                        {-1, 2},
                        {-1, -2},
                };

                for (int[] move : knight_moves) {
                    int new_row = row + move[0];
                    int new_col = col + move[1];
                }

            case ROOK:



            case QUEEN:
                for (int row_offset = 1; row_offset <= 8; row_offset++) {
                    for (int col_offset = 1; col_offset <= 8; col_offset++) {
                        int new_row = row + row_offset;
                        int new_col = col + col_offset;
                    }
                }



        }



    }

    private Collection<ChessMove> getMove(ChessBoard board, ChessPosition myPosition, ChessPosition endPosition, ChessPiece.PieceType promotionPiece) {
        int row = myPosition.getRow();
        int col = myPosition.getColumn();
        for (int [] dir : directions )
    }

}
