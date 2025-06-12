package ui;

/**
 * This class contains constants and methods for generating ANSI escape sequences
 * for terminal output.
 */
public class EscapeSequences {
    private static final String UNICODE_WHITE_KING = "\u2654";
    private static final String UNICODE_WHITE_QUEEN = "\u2655";
    private static final String UNICODE_WHITE_BISHOP = "\u2656";
    private static final String UNICODE_WHITE_KNIGHT = "\u2658";
    private static final String UNICODE_WHITE_ROOK = "\u2656"; // Correction: Should be \u2656 or \u2657
    private static final String UNICODE_WHITE_PAWN = "\u2659";

    // Fixed typo for ROOK and added proper UNICODE_WHITE_ROOK
    private static final String UNICODE_WHITE_ROOK_FIXED = "\u2656"; // Bishop
    private static final String UNICODE_BLACK_ROOK_FIXED = "\u265c"; // Black Rook

    public static final String UNICODE_WHITE_ROOK_CORRECT = "\u2656"; // This was Bishop. Correct is \u2656
    public static final String UNICODE_WHITE_KNIGHT_CORRECT = "\u2658";
    public static final String UNICODE_WHITE_BISHOP_CORRECT = "\u2657"; // Correct is \u2657

    // Corrected Unicode for Rook and Bishop
    public static final String WHITE_KING = "\u2654";
    public static final String WHITE_QUEEN = "\u2655";
    public static final String WHITE_ROOK = "\u2656"; // ♖
    public static final String WHITE_KNIGHT = "\u2658"; // ♘
    public static final String WHITE_BISHOP = "\u2657"; // ♗
    public static final String WHITE_PAWN = "\u2659"; // ♙

    public static final String BLACK_KING = "\u265A"; // ♚
    public static final String BLACK_QUEEN = "\u265B"; // ♛
    public static final String BLACK_ROOK = "\u265C"; // ♜
    public static final String BLACK_KNIGHT = "\u265E"; // ♞
    public static final String BLACK_BISHOP = "\u265D"; // ♝
    public static final String BLACK_PAWN = "\u265F"; // ♟

    public static final String EMPTY = " \u2003 "; // Espacio en blanco unicode para celdas vacías

    private static final String ESCAPE = "\033";
    private static final String SET_TEXT_COLOR = ESCAPE + "[38;5;";
    private static final String SET_BG_COLOR = ESCAPE + "[48;5;";
    private static final String SET_BG_COLOR_TRUE_COLOR = ESCAPE + "[48;2;"; // New for true color
    private static final String SET_TEXT_COLOR_TRUE_COLOR = ESCAPE + "[38;2;"; // New for true color

    public static final String SET_TEXT_COLOR_BLACK = ESCAPE + "[30m"; // Black foreground
    public static final String SET_TEXT_COLOR_WHITE = ESCAPE + "[37m"; // White foreground
    public static final String SET_TEXT_COLOR_RED = ESCAPE + "[31m";
    public static final String SET_TEXT_COLOR_GREEN = ESCAPE + "[32m";
    public static final String SET_TEXT_COLOR_YELLOW = ESCAPE + "[33m";
    public static final String SET_TEXT_COLOR_BLUE = ESCAPE + "[34m";
    public static final String SET_TEXT_COLOR_MAGENTA = ESCAPE + "[35m";
    public static final String SET_TEXT_COLOR_CYAN = ESCAPE + "[36m";
    public static final String SET_TEXT_COLOR_BRIGHT_WHITE = ESCAPE + "[97m";


    public static final String RESET_TEXT_COLOR = ESCAPE + "[39m";
    public static final String RESET_BG_COLOR = ESCAPE + "[49m";
    public static final String RESET_ALL = ESCAPE + "[0m";

    public static final String ERASE_SCREEN = ESCAPE + "[2J";
    public static final String SET_CURSOR_TO_HOME_POSITION = ESCAPE + "[H";

    // Common background colors for chess board
    public static final String SET_BG_COLOR_DARK_GREY = SET_BG_COLOR + "235m"; // Dark grey
    public static final String SET_BG_COLOR_LIGHT_GREY = SET_BG_COLOR + "250m"; // Light grey

    // Colors for selected squares or highlight
    public static final String SET_BG_COLOR_GREEN = ESCAPE + "[42m"; // Standard green
    public static final String SET_BG_COLOR_BRIGHT_GREEN = ESCAPE + "[102m"; // Bright green (lighter for checkerboard)
    public static final String SET_BG_COLOR_RED = ESCAPE + "[41m"; // Standard red
    public static final String SET_BG_COLOR_YELLOW = ESCAPE + "[43m"; // Standard yellow

    public static String setColor(int r, int g, int b) {
        return SET_TEXT_COLOR_TRUE_COLOR + r + ";" + g + ";" + b + "m";
    }

    public static String setBackgroundColor(int r, int g, int b) {
        return SET_BG_COLOR_TRUE_COLOR + r + ";" + g + ";" + b + "m";
    }
}