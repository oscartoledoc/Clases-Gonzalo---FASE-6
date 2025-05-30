package ui;


import chess.*;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.Scanner;

public class Client {
    private final String serverURL;
    private final ServerFacade serverFacade;
    private boolean isRunning;
    private boolean isLoggedIn;
    private ChessBoard board;
    private String currentPlayerColor;
    private String authToken;
    private String currentGameId;
    private final Gson gson = new Gson();
    private WebSocketClient wsClient;
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public Client(String serverURL) {
        this.authToken = null;
        this.serverURL = serverURL;
        this.serverFacade = new ServerFacade(serverURL);
        this.wsClient = new WebSocketClient(this);
        this.isRunning = true;
        this.isLoggedIn = false;
        this.board = new ChessBoard();
        this.board.resetBoard();
        this.currentPlayerColor = null;
    }

    public void run() {
        System.out.println("Welcome to 240 Chess! Type Help to get started");
        displayBoard();
        Scanner scanner = new Scanner(System.in);
        while(isRunning) {
            String command = scanner.nextLine().trim().toLowerCase();
            handleCommand(command, scanner);
        }
        scanner.close();
    }

    private void handleCommand(String command, Scanner scanner) {
        if (isLoggedIn) {
            handlePostLoginCommand(command, scanner);
        } else {
            handlePreLoginCommand(command, scanner);
        }
    }


    private void handlePreLoginCommand(String command, Scanner scanner) {
        switch (command) {
            case "help":
                displayHelp();
                break;
            case "quit":
                isRunning = false;
                System.out.println("Goodbye!");
                break;
            case "login":
                handleLogin(scanner);
                break;
            case "register":
                handleRegister(scanner);
                break;
            default:
                System.out.println("Invalid command, type Help for a list of commands");
        }
    }

    private void handlePostLoginCommand(String command, Scanner scanner) {
        switch (command) {
            case "help":
                displayHelp();
                break;
            case "logout":
                handleLogout();
                break;
            case "create game":
                handleCreateGame(scanner);
                break;
            case "list games":
                handleListGames();
                break;
            case "play game":
                handleJoinGame(scanner);
                break;
            case "observe game":
                handleObserveGame(scanner);
                break;
            default:
                System.out.println("Invalid command, type Help for a list of commands");
        }
    }

    private void handleMove(Scanner scanner) {
        if (currentGameId == null) {
            System.out.println("You must join a game first");
            return;
        }
        System.out.println("Enter move (e.g., e2-e4): ");
        String moveStr = scanner.nextLine().trim();
        String[] positions = moveStr.split("-");
        if (positions.length != 2) {
            System.out.println("Invalid move format");
            return;
        }
        try {
            ChessPosition start = new ChessPosition(Integer.parseInt(positions[0].substring(1)), positions[0].charAt(0) - 'a' + 1);
            ChessPosition end = new ChessPosition(Integer.parseInt(positions[1].substring(1)), positions[1].charAt(0) - 'a' + 1);
            ChessMove move = new ChessMove(start, end, null);
            wsClient.sendMove(gson.toJson(move));
            System.out.println("Move sent: " + moveStr);
        } catch (Exception e) {
            System.out.println("Invalid move: " + e.getMessage());
        }
    }

    private void handleResign() {
        if (currentGameId == null) {
            System.out.println("You must join a game first");
            return;
        }
        try {
            wsClient.sendResign();
            System.out.println("Resigned from game");
            currentGameId = null;
            currentPlayerColor = null;
        } catch (IOException e) {
            System.out.println("Resign failed: " + e.getMessage());
        }
    }

    private void handleLeave() {
        if (currentGameId == null) {
            System.out.println("You must join a game first");
            return;
        }
        try {
            wsClient.sendLeave();
            System.out.println("Left game");
            currentGameId = null;
            currentPlayerColor = null;
        } catch (IOException e) {
            System.out.println("Leave failed: " + e.getMessage());
        }
    }


    private void displayHelp() {
        System.out.println("Available commands: ");
        System.out.println("Help - Displays this help message");
        if (isLoggedIn){
            System.out.println("Logout - Logs out of the current account");
            System.out.println("Create Game - Creates a new chess game");
            System.out.println("List Games - Lists existing chess games");
            System.out.println("Play Game - Join an available chess game");
            System.out.println("Observe Game - Observe an ongoing chess game");
        } else {
            System.out.println("Login - Logs in to an existing account");
            System.out.println("Register - Registers a new user");
            System.out.println("Quit - Exit the client");
        }
    }

    private void handleLogin(Scanner scanner) {
        System.out.println("Enter username: ");
        String username = scanner.nextLine().trim();
        System.out.println("Enter password: ");
        String password = scanner.nextLine().trim();
        try {
            String response = serverFacade.login(username, password);
            System.out.println("Login successful " + response);
        } catch (Exception e){
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    private void handleRegister(Scanner scanner) {
        System.out.println("Enter username: ");
        String username = scanner.nextLine().trim();
        System.out.println("Enter password: ");
        String password = scanner.nextLine().trim();
        System.out.println("Enter email: ");
        String email = scanner.nextLine().trim();
        try {
            String response = serverFacade.register(username, password, email);
            System.out.println("Registration successful " + response);
        } catch (Exception e){
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    private void handleLogout() {
        String response = null;
        try {
            response = serverFacade.logout();
        } catch (Exception e){
            System.out.println("Logout failed: " + e.getMessage());
        }
        authToken = null;
        isLoggedIn = false;
        System.out.println("Logout successful " + response);
        displayBoard();
    }

    private void handleCreateGame(Scanner scanner) {
        if (!loginCheck()){
            return;
        }
        System.out.println("Enter game name: ");
        String gameName = scanner.nextLine().trim();
        try {
            String response = serverFacade.createGame(gameName);
            System.out.println("Game created " + response);
        } catch (Exception e){
            System.out.println("Game creation failed: " + e.getMessage());
        }
    }

    private void handleListGames(){
        if (!loginCheck()){
            return;
        }
        try {
            String response = serverFacade.listGames();
            System.out.println("Games: " + response);
        } catch (Exception e){
            System.out.println("Game listing failed: " + e.getMessage());
        }
    }

    private void handleJoinGame(Scanner scanner) {
        if (!loginCheck()){
            return;
        }
        System.out.println("Enter game id: ");
        String gameId = scanner.nextLine().trim();
        System.out.println("Enter player color (white/black): ");
        String playerColor = scanner.nextLine().trim().toLowerCase();
        if (!playerColor.equals("white") && !playerColor.equals("black")) {
            System.out.println("Invalid player color, must be white or black");
            return;

        }
        try {
            String response = serverFacade.joinGame(gameId, playerColor);
            currentPlayerColor = playerColor;
            currentGameId = gameId;
            wsClient.connect("ws://localhost:8081", authToken, gameId);
            System.out.println("Game joined " + response);
            displayBoard();
        } catch (Exception e){
            System.out.println("Game joining failed: " + e.getMessage());
        }

    }
    private void handleObserveGame(Scanner scanner) {
        if (!loginCheck()) return;
        System.out.println("Enter game id: ");
        String gameId = scanner.nextLine().trim();
        try {
            currentGameId = gameId;
            wsClient.connect("ws://localhost:8081", authToken, gameId);
            System.out.println("Observing game " + gameId);
            displayBoard();
        } catch (Exception e) {
            System.out.println("Observing failed: " + e.getMessage());
        }
    }


    private boolean loginCheck() {
        if (!isLoggedIn) {
            System.out.println("You must be logged in to perform this action");
            return false;
        }
        return true;
    }


    private void displayBoard() {
        System.out.println(EscapeSequences.ERASE_SCREEN);
        for (int row = 8; row >= 1; row--) {
            System.out.print(row + " ");
            for (int col = 1; col <= 8; col++) {
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(pos);
                String pieceSymbol = piece != null ? getPieceSymbol(piece): EscapeSequences.EMPTY;
                String bgColor = ((row + col)% 2 == 0)? EscapeSequences.SET_BG_COLOR_LIGHT_GREY : EscapeSequences.SET_BG_COLOR_DARK_GREY;
                System.out.println(bgColor + pieceSymbol + EscapeSequences.RESET_BG_COLOR);
            }
            System.out.println();
        }
        System.out.println("  a b c d e f g h");
        if (isLoggedIn && currentPlayerColor != null) {
            System.out.println("Current player: " + currentPlayerColor);
        } else {
            System.out.println("Not in a game or not logged in");
        }
    }

    private String getPieceSymbol(ChessPiece piece) {
        ChessGame.TeamColor color = piece.getTeamColor();
        ChessPiece.PieceType type = piece.getPieceType();
        switch (type) {
            case PAWN:
                return color == ChessGame.TeamColor.WHITE ? EscapeSequences.WHITE_PAWN : EscapeSequences.BLACK_PAWN;
            case KNIGHT:
                return color == ChessGame.TeamColor.WHITE ? EscapeSequences.WHITE_KNIGHT : EscapeSequences.BLACK_KNIGHT;
            case BISHOP:
                return color == ChessGame.TeamColor.WHITE ? EscapeSequences.WHITE_BISHOP : EscapeSequences.BLACK_BISHOP;
            case ROOK:
                return color == ChessGame.TeamColor.WHITE ? EscapeSequences.WHITE_ROOK : EscapeSequences.BLACK_ROOK;
            case QUEEN:
                return color == ChessGame.TeamColor.WHITE ? EscapeSequences.WHITE_QUEEN : EscapeSequences.BLACK_QUEEN;
            case KING:
                return color == ChessGame.TeamColor.WHITE ? EscapeSequences.WHITE_KING : EscapeSequences.BLACK_KING;
            default:
                return EscapeSequences.EMPTY;
        }
    }

    public static void main(String[] args) {
        String serverURL = "http://localhost:8080";
        Client client = new Client(serverURL);
        client.run();
    }



}
