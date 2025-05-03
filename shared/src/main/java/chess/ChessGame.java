package chess;

import java.util.ArrayList;
import java.util.Collection;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {

    private ChessBoard board;
    private TeamColor teamTurn;

    private boolean whiteKingMove;
    private boolean blackKingMove;
    private boolean whiteRookRMove;
    private boolean whiteRookLMove;
    private boolean blackRookRMove;
    private boolean blackRookLMove;

    public ChessGame() {
        board = new ChessBoard();
        board.resetBoard();
        teamTurn = TeamColor.WHITE;
        whiteKingMove = false;
        blackKingMove = false;
        whiteRookRMove = false;
        whiteRookLMove = false;
        blackRookRMove = false;
        blackRookLMove = false;

    }

    private boolean isUnderAttack (ChessPosition position, TeamColor teamColor) {
        TeamColor opponentColor = (teamColor == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition enemyPosition = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(enemyPosition);
                if (piece != null && piece.getTeamColor() == teamColor) {
                    Collection<ChessMove> enemyMoves = piece.pieceMoves(board, enemyPosition);
                    for (ChessMove move : enemyMoves) {
                        if (move.getEndPosition().equals(position)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isRoadClear (ChessPosition kingStart, ChessPosition rookStart) {
        int row = kingStart.getRow();
        int kingCol = kingStart.getColumn();
        int rookCol = rookStart.getColumn();

        int startCol = Math.min(kingCol, rookCol) + 1;
        int endCol = Math.max(kingCol, rookCol) - 1;

        if (rookCol == 1) {
            for (int col = startCol; col <= endCol; col++) {
                if (board.getPiece(new ChessPosition(row, col)) != null) {
                    return false;
                }
            }
        }

        if (rookCol == 8) {
            for (int col = startCol; col >= endCol; col--) {
                if (board.getPiece(new ChessPosition(row, col)) != null) {
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        this.teamTurn = team;

    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);



        if (piece == null) {
            return null;
        }
        Collection<ChessMove> possibleMoves = piece.pieceMoves(board, startPosition);
        Collection<ChessMove> validMoves = new ArrayList<>();

        for (ChessMove move : possibleMoves) {
            ChessPiece targetPiece = board.getPiece(move.getEndPosition());
            if (targetPiece != null && targetPiece.getTeamColor() == piece.getTeamColor()) {
                continue;
            }
            ChessPiece pieceToPlace = piece;

            if (piece.getPieceType() == ChessPiece.PieceType.PAWN && move.getPromotionPiece() != null) {
                pieceToPlace = new ChessPiece(piece.getTeamColor(), move.getPromotionPiece());
            }

            boolean isValidCastlingMove = true;

            if (piece.getPieceType() == ChessPiece.PieceType.KING) {
                int colDiff = move.getEndPosition().getColumn() - startPosition.getColumn();

                if (Math.abs(colDiff) == 2) {
                    int row = startPosition.getRow();
                    ChessPosition rookStart = colDiff == 2 ?
                        new ChessPosition(row, 8) : new ChessPosition(row, 1);

                    ChessPiece rook = board.getPiece(rookStart);
                    if (rook == null || rook.getTeamColor() != piece.getTeamColor()) {
                        isValidCastlingMove = false;
                    } else {
                        if (piece.getTeamColor() == TeamColor.WHITE) {
                            if (whiteKingMove) {
                                isValidCastlingMove = false;
                            } else if (colDiff == 2 && whiteRookRMove) {
                                isValidCastlingMove = false;
                            } else if (colDiff == -2 && whiteRookLMove) {
                                isValidCastlingMove = false;
                            }
                        } else {
                            if (blackKingMove) {
                                isValidCastlingMove = false;
                            } else if (colDiff == 2 && blackRookRMove) {
                                isValidCastlingMove = false;
                            } else if (colDiff == -2 && blackRookLMove) {
                                isValidCastlingMove = false;
                            }
                        }
                    }
                    if (isValidCastlingMove) {
                       int kingStartCol = startPosition.getColumn();
                       int direction = colDiff > 0 ? 1 : -1;
                       for (int col = kingStartCol; col != kingStartCol + colDiff + direction; col += direction) {
                           ChessPosition pos = new ChessPosition(row, col);
                           if (col == kingStartCol + colDiff + direction) break;
                           if (isUnderAttack(pos, piece.getTeamColor())) {
                               isValidCastlingMove = false;
                               break;
                           }
                       }
                    }
                    if (isValidCastlingMove) {
                        if (!isRoadClear(startPosition, rookStart)) {
                            isValidCastlingMove = false;
                        }
                    }
                }
            }

            ChessPiece originalTarget = board.getPiece(move.getEndPosition());

            board.addPiece(move.getEndPosition(), pieceToPlace);
            board.addPiece(startPosition, null);

            boolean inCheck = isInCheck(piece.getTeamColor());

            board.addPiece(startPosition, piece);
            board.addPiece(move.getEndPosition(), originalTarget);

            if (!inCheck) {
                validMoves.add(move);
            }

        }
        return validMoves;

    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {

        if (move == null) {
            throw new InvalidMoveException("Move cannot be null");
        }
        ChessPosition startPosition = move.getStartPosition();
        ChessPosition endPosition = move.getEndPosition();
        ChessPiece piece = board.getPiece(startPosition);

        if (piece == null) {
            throw new InvalidMoveException("No piece at start position");
        }

        if (piece.getTeamColor() != teamTurn) {
            throw new InvalidMoveException("Not your turn");
        }

        ChessPiece targetPiece = board.getPiece(endPosition);

        if (targetPiece != null && targetPiece.getTeamColor() == piece.getTeamColor()){
            throw new InvalidMoveException("Cannot capture own piece");
        }

        if (piece.getPieceType() == ChessPiece.PieceType.KING) {
            int row = startPosition.getRow();
            int colDiff = endPosition.getColumn() - startPosition.getColumn();
            if (Math.abs(colDiff) == 2) {
                ChessPosition rookStartPos;
                ChessPosition rookEndPos;
                if (colDiff == 2) {
                    rookStartPos = new ChessPosition(row, 8);
                    rookEndPos = new ChessPosition(row, 6);
                } else {
                    rookStartPos = new ChessPosition(row, 1);
                    rookEndPos = new ChessPosition(row, 4);
                }
                ChessPiece rook = board.getPiece(rookStartPos);
                if (rook == null) {
                    throw new InvalidMoveException("Rook not found");
                }
                board.addPiece(rookEndPos, rook);
                board.addPiece(rookStartPos, null);
            }

        }

        if (piece.getPieceType() == ChessPiece.PieceType.KING) {
            if (piece.getTeamColor() == TeamColor.WHITE) {
                 whiteKingMove = true;
            } else {
                blackKingMove = true;
            }
        } else if (piece.getPieceType() == ChessPiece.PieceType.ROOK) {
            if (piece.getTeamColor() == TeamColor.WHITE) {
                if (startPosition.equals(new ChessPosition(1, 8))) {
                    whiteRookLMove = true;
                } else if (startPosition.equals(new ChessPosition(1, 1))) {
                    whiteRookRMove = true;
                }
            } else {
                if (startPosition.equals(new ChessPosition(8, 8))) {
                    blackRookLMove = true;
                } else if (startPosition.equals(new ChessPosition(8, 1))) {
                    blackRookRMove = true;
                }
            }
        }


        Collection<ChessMove> validMoves = piece.pieceMoves(board, startPosition);

        boolean isValidMove = false;

        for (ChessMove validMove : validMoves) {
            if (validMove.equals(move)) {
                isValidMove = true;
                break;
            }
        }

        if (!isValidMove) {
            throw new InvalidMoveException("Invalid move");
        }

        ChessPiece pieceToPlace = piece;

        if (piece.getPieceType() == ChessPiece.PieceType.PAWN && move.getPromotionPiece() != null) {
            if ((piece.getTeamColor() == TeamColor.WHITE && endPosition.getRow() == 8) || (piece.getTeamColor() == TeamColor.BLACK && endPosition.getRow() == 1)) {
                pieceToPlace = new ChessPiece(piece.getTeamColor(), move.getPromotionPiece());
            }
        }


        board.addPiece(endPosition, pieceToPlace);
        board.addPiece(startPosition, null);

        if (isInCheck(piece.getTeamColor())) {
            board.addPiece(startPosition, piece);
            board.addPiece(endPosition, targetPiece);
            throw new InvalidMoveException("Move leaves king in check");
        }

        teamTurn = (teamTurn == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;


    }



    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition kingPosition = null;
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);
                if (piece != null && piece.getTeamColor() == teamColor && piece.getPieceType() == ChessPiece.PieceType.KING) {
                    kingPosition = position;
                    break;
                }
            }
            if (kingPosition != null) {
                break;
            }
        }
        if (kingPosition == null) {
            return false;
        }
        TeamColor opponentColor = (teamColor == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);
                if (piece != null && piece.getTeamColor() == opponentColor) {
                    Collection<ChessMove> moves = piece.pieceMoves(board, position);
                    for (ChessMove move : moves) {
                        if (move.getEndPosition().equals(kingPosition)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;

    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        if (!isInCheck(teamColor)) return false;
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);
                if (piece != null && piece.getTeamColor() == teamColor) {
                    Collection<ChessMove> moves = piece.pieceMoves(board, position);
                    for (ChessMove move : moves) {
                        ChessPiece targetPiece = board.getPiece(move.getEndPosition());
                        ChessPiece pieceToPlace = piece;
                        if (piece.getPieceType() == ChessPiece.PieceType.PAWN && move.getPromotionPiece() != null) {
                            pieceToPlace = new ChessPiece(piece.getTeamColor(), move.getPromotionPiece());
                        }
                        board.addPiece(move.getEndPosition(), pieceToPlace);
                        board.addPiece(position, null);

                        boolean stillInCheck = isInCheck(teamColor);
                        board.addPiece(position, piece);
                        board.addPiece(move.getEndPosition(), targetPiece);
                        if (!stillInCheck) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        if (isInCheck(teamColor)) return false;
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);
                if (piece != null && piece.getTeamColor() == teamColor) {
                    Collection<ChessMove> moves = piece.pieceMoves(board, position);
                    for (ChessMove move : moves) {
                        ChessPiece targetPiece = board.getPiece(move.getEndPosition());
                        ChessPiece pieceToPlace = piece;
                        if (piece.getPieceType() == ChessPiece.PieceType.PAWN && move.getPromotionPiece() != null) {
                            pieceToPlace = new ChessPiece(piece.getTeamColor(), move.getPromotionPiece());
                        }
                        board.addPiece(move.getEndPosition(), pieceToPlace);
                        board.addPiece(position, null);

                        boolean inCheckAfterMove = isInCheck(teamColor);
                        board.addPiece(position, piece);
                        board.addPiece(move.getEndPosition(), targetPiece);
                        if (!inCheckAfterMove) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return board;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChessGame other = (ChessGame) o;

        return teamTurn == other.teamTurn && board.equals(other.board);


    }

    @Override
    public int hashCode() {
        int result = teamTurn.hashCode();
        result = 31 * result + board.hashCode();
        return result;

    }


}
