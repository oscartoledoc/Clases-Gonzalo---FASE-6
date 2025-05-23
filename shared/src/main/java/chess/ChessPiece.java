package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    private final ChessGame.TeamColor pieceColor;
    private ChessPiece.PieceType type;

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

        Collection<ChessMove> moves = new ArrayList<>();

        if (board == null || myPosition == null) {return moves;}

        int row = myPosition.getRow();
        int col = myPosition.getColumn();

        switch (type) {
            case PAWN:
                int direction = pieceColor == ChessGame.TeamColor.WHITE ? 1 : -1;
                int startRow = pieceColor == ChessGame.TeamColor.WHITE ? 2 : 7;

                if (row == startRow && isValidPosition(row + 2 * direction, col)) {
                    ChessPosition oneStep = new ChessPosition(row + direction, col);
                    ChessPosition twoStep = new ChessPosition(row + 2 * direction, col);
                    if (board.getPiece(oneStep) == null && board.getPiece(twoStep) == null) {
                        moves.add(new ChessMove(myPosition, twoStep, null));
                    }
                }

                if (isValidPosition(row + direction, col)) {
                    ChessPosition newPos = new ChessPosition(row + direction, col);
                    if (board.getPiece(newPos) == null) {
                        if (row + direction == (pieceColor == ChessGame.TeamColor.WHITE ? 8 : 1)) {
                            moves.add(new ChessMove(myPosition, newPos, ChessPiece.PieceType.QUEEN));
                            moves.add(new ChessMove(myPosition, newPos, ChessPiece.PieceType.ROOK));
                            moves.add(new ChessMove(myPosition, newPos, ChessPiece.PieceType.BISHOP));
                            moves.add(new ChessMove(myPosition, newPos, ChessPiece.PieceType.KNIGHT));
                        }
                        else {
                            moves.add(new ChessMove(myPosition, newPos, null));
                        }
                    }
                }
                generatePawnCaptures(moves, row, col, pieceColor, board, myPosition);

                break;

            case KING:
                for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
                    for (int colOffset = -1; colOffset <= 1; colOffset++) {

                        if (rowOffset == 0 && colOffset == 0) {
                            continue;
                        }

                        int newRow = row + rowOffset;
                        int newCol = col + colOffset;

                        tryAddMove(moves, newRow, newCol, board, myPosition);


                    }
                }
                break;

            case KNIGHT:
                int [][] knightMoves = {{2, 1}, {2, -1}, {-2, 1}, {-2, -1}, {1, 2}, {1, -2}, {-1, 2}, {-1, -2}};

                for (int[] move : knightMoves) {
                    int newRow = row + move[0];
                    int newCol = col + move[1];

                    tryAddMove(moves, newRow, newCol, board, myPosition);
                }
                break;

            case ROOK:
                moves.addAll(getMove(board, myPosition, new int[]{0, 1}));
                moves.addAll(getMove(board, myPosition, new int[]{1, 0}));
                moves.addAll(getMove(board, myPosition, new int[]{0, -1}));
                moves.addAll(getMove(board, myPosition, new int[]{-1, 0}));
                break;

            case QUEEN:
                moves.addAll(getMove(board, myPosition, new int[]{0, 1}, new int[] {1, 0}));
                moves.addAll(getMove(board, myPosition, new int[]{0, -1}, new int[] {-1, 0}));
                moves.addAll(getMove(board, myPosition, new int[]{1, 1}, new int[] {1, -1}));
                moves.addAll(getMove(board, myPosition, new int[]{-1, 1}, new int[] {-1, -1}));
                break;

            case BISHOP:
                moves.addAll(getMove(board, myPosition, new int[]{1, 1}, new int[] {1, -1}));
                moves.addAll(getMove(board, myPosition, new int[]{-1, 1}, new int[] {-1, -1}));
                break;

        }
        return moves;
    }

    private boolean isValidPosition(int row, int col) {
        return row >= 1 && row <= 8 && col >= 1 && col <= 8;
    }

    private void tryAddMove(Collection<ChessMove> moves, int newRow, int newCol, ChessBoard board, ChessPosition myPosition) {
        if (isValidPosition(newRow, newCol)) {
            ChessPosition newPos = new ChessPosition(newRow, newCol);
            ChessPiece targetPiece = board.getPiece(newPos);
            if (targetPiece == null || targetPiece.getTeamColor() != pieceColor) {
                moves.add(new ChessMove(myPosition, newPos, null));
            }
        }
    }

    private void generatePawnCaptures(Collection<ChessMove> moves, int row, int col,
                                      ChessGame.TeamColor pieceColor, ChessBoard board,
                                      ChessPosition myPosition) {
        int direction = (pieceColor == ChessGame.TeamColor.WHITE) ? 1 : -1;
        int newRow = row + direction;

        for (int colOffset : new int[]{-1, 1}) {
            int newCol = col + colOffset;
            if (!isValidPosition(newRow, newCol)) {
                continue;
            }

            ChessPosition capturePos = new ChessPosition(newRow, newCol);
            ChessPiece targetPiece = board.getPiece(capturePos);
            if (targetPiece == null || targetPiece.getTeamColor() == pieceColor) {
                continue;
            }

            if (newRow == (pieceColor == ChessGame.TeamColor.WHITE ? 8 : 1)) {
                moves.add(new ChessMove(myPosition, capturePos, PieceType.QUEEN));
                moves.add(new ChessMove(myPosition, capturePos, PieceType.ROOK));
                moves.add(new ChessMove(myPosition, capturePos, PieceType.BISHOP));
                moves.add(new ChessMove(myPosition, capturePos, PieceType.KNIGHT));
            }   else {
                moves.add(new ChessMove(myPosition, capturePos, null));
            }
        }
    }

    private Collection<ChessMove> getMove(ChessBoard board, ChessPosition myPosition, int[]... directions) {

        List<ChessMove> moves = new ArrayList<>();

        int row = myPosition.getRow();
        int col = myPosition.getColumn();

        for (int [] dir : directions){
            int rowDir = dir[0];
            int colDir = dir[1];

            int newRow = row;
            int newCol = col;

            while (true) {
                newRow += rowDir;
                newCol += colDir;

                if (!isValidPosition(newRow, newCol)) {
                    break;
                }

                ChessPosition newPos = new ChessPosition(newRow, newCol);
                ChessPiece targetPiece = board.getPiece(newPos);

                if (targetPiece == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                }
                else
                {
                    if (targetPiece.getTeamColor() != pieceColor) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }
                    break;
                }
            }
        }
        return moves;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessPiece that = (ChessPiece) o;
        return pieceColor == that.pieceColor &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        int result = pieceColor.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

}
