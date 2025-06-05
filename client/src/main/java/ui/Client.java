package ui;

import java.io.IOException;
import java.util.Scanner;
import chess.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import websocket.*;

public class Client {
    private final String serverURL;
    private final ServerFacade serverFacade;
    private final WebSocketClient wsClient;
    private boolean isRunning;
    private boolean isLoggedIn;
    private ChessBoard board;
    private String currentPlayerColor;
    private String authToken;

    public Client(String serverURL) {
        this.serverURL = serverURL;
        this.serverFacade = new ServerFacade(serverURL);
        this.wsClient = new WebSocketClient();
        this.isRunning = true;
        this.isLoggedIn = false;
        this.board = new ChessBoard();
        this.board.resetBoard();
        this.currentPlayerColor = null;

        wsClient.setListener(new WebSocketClient.clientListener() {
            @Override
            public void onGameUpdate(ChessGame game) {
                board = game.getBoard();
                displayBoard();
            }

            @Override
            public void onNotification(String message) {
                System.out.println("Notification: " + message);
            }

            @Override
            public void onError(String message) {
                System.out.println("Error: " + message);
            }
        });
    }

    public void run() {
        System.out.println("Welcome to 240 Chess! Type Help to get started");
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
            case "join game":
                handleJoinGame(scanner);
            case "observe game":
                handleObserveGame(scanner);
                break;
            default:
                System.out.println("Invalid command, type Help for a list of commands");
        }
    }

    // private void handleCommand(String command, Scanner scanner) {
    //     switch (command) {
    //         case "help":
    //             displayHelp();
    //             break;
    //         case "quit":
    //             isRunning = false;
    //             System.out.println("Goodbye!");
    //             break;
    //         case "login":
    //             handleLogin(scanner);
    //             break;
    //         case "register":
    //             handleRegister(scanner);
    //             break;
    //             default:
    //                System.out.println("Invalid command, type Help for a list of commands");
    //     }
    // }


    private void displayHelp() {
        System.out.println("Available commands: ");
        System.out.println("Help - Displays this help message");
        if (isLoggedIn){
            System.out.println("Logout - Logs out of the current account");
            System.out.println("Create Game - Creates a new chess game");
            System.out.println("List Games - Lists existing chess games");
            System.out.println("Join Game - Join an available chess game");
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
            serverFacade.login(username, password);
            System.out.println("Login successful!");
            isLoggedIn = true;
            authToken = serverFacade.getAuthToken();
            System.out.println("You are now logged in");
            System.out.println("Type Help for a list of commands");
        } catch (IOException e){
            if (e.getMessage().contains("401")) {
                System.out.println("Login failed: Invalid username or password");
            }
            else {
                System.out.println("Login failed: " + e.getMessage());
            }
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
            serverFacade.register(username, password, email);
            System.out.println("Registration successful!");
            System.out.println("Type Help for a list of commands");
            serverFacade.login(username, password);
            isLoggedIn = true;
            authToken = serverFacade.getAuthToken();
        } catch (IOException e){
            if (e.getMessage().contains("403")) {
                System.out.println("Registration failed: Username already exists");
            }
            else {
                System.out.println("Registration failed: " + e.getMessage());
            }
        }
    }

    private void handleLogout() {
        try {
            serverFacade.logout();
        } catch (Exception e){
            System.out.println("Logout failed: " + e.getMessage());
        }
        authToken = null;
        isLoggedIn = false;
        System.out.println("Logout successful!");
    }

    private void handleCreateGame(Scanner scanner) {
        if (!loginCheck()){
            return;
        }
        System.out.println("Enter game name: ");
        String gameName = scanner.nextLine().trim();
        try {
            serverFacade.createGame(gameName);
            System.out.println("Game created successfully!");
        } catch (Exception e){
            System.out.println("Game creation failed: " + e.getMessage());
        }
    }

    private void handleListGames() {
        if(!loginCheck()) {
            return;
        }
        try {
            String response = serverFacade.listGames();
            Gson gson = new Gson();
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            JsonArray gamesArray = jsonResponse.getAsJsonArray("games");

            if (gamesArray == null || gamesArray.size() == 0) {
                System.out.println("No games available at the moment.");
                return;
            }

            System.out.println("Available Games:");
            int index = 1;
            for (JsonElement gameElement : gamesArray) {
                JsonObject game = gameElement.getAsJsonObject();
                String gameName = game.get("gameName").getAsString();

                String whitePlayer = game.has("whiteUsername") && !game.get("whiteUsername").isJsonNull()
                        ? game.get("whiteUsername").getAsString() : "None";

                String blackPlayer = game.has("blackUsername") && !game.get("blackUsername").isJsonNull()
                        ? game.get("blackUsername").getAsString() : "None";

                System.out.printf("%d. %s - White: %s, Black: %s%n",
                        index, gameName, whitePlayer, blackPlayer);
                index++;
            }

        } catch (IOException e) {
            System.out.println("Failed to list games: Unable to connect to the server. Please try again later.");
        } catch (Exception e) {
            System.out.println("Failed to list games: An unexpected error occurred. Please try again.");
        }
    }

    private void handleJoinGame(Scanner scanner) {
        if (!loginCheck()) {
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
            serverFacade.joinGame(gameId, playerColor);
            currentPlayerColor = playerColor;
            System.out.println("Game joined");
            displayBoard();
        } catch (IOException e){
            if (e.getMessage().contains("403")) {
                System.out.println("Unable to join: Color taken");
            } else if (e.getMessage().contains("500")) {
                System.out.println("Unable to join: ID not found");
            }
            else {
                System.out.println("Unable to join: " + e.getMessage());
            }
        }

    }
    private void handleObserveGame(Scanner scanner) {
        return;
    }

    private void handleMakeMove(Scanner scanner) {
        if (currentPlayerColor == null) {
            System.out.println("Observers cannot make moves.");
            return;
        }
        System.out.println("Enter move (e.g., e2 e4): ");
        String moveInput = scanner.nextLine().trim();
        String[] parts = moveInput.split("\\s+");
        if (parts.length != 2) {
            System.out.println("Invalid move format. Use: start end (e.g., e2 e4)");
            return;
        }
        try {
            ChessPosition start = parsePosition(parts[0]);
            ChessPosition end = parsePosition(parts[1]);
            ChessMove move = new ChessMove(start, end, null);
            sendMove(move);
        } catch (Exception e) {
            System.out.println("Invalid move: " + e.getMessage());
        }
    }

    private void handleResign() {
        if (currentPlayerColor == null) {
            System.out.println("Observers cannot resign.");
            return;
        }
        sendResign();
        inGame = false;
        currentGameId = null;
        currentPlayerColor = null;
        wsClient.disconnect();
        System.out.println("You have resigned. Type Help for commands.");
    }

    private void handleLeave() {
        sendLeave();
        inGame = false;
        currentGameId = null;
        currentPlayerColor = null;
        wsClient.disconnect();
        System.out.println("You have left the game. Type Help for commands.");
    }

    private void sendMove(ChessMove move) {
        if (currentPlayerColor == null) {
            System.out.println("Observers cannot make moves.");
            return;
        }
        UserGameCommand command = new MakeMoveCommand(authToken, Integer.parseInt(currentGameId), move);
        wsClient.sendCommand(command);
    }

    private void sendResign() {
        if (currentPlayerColor == null) {
            System.out.println("Observers cannot resign.");
            return;
        }
        UserGameCommand command = new UserGameCommand(UserGameCommand.CommandType.RESIGN, authToken, Integer.parseInt(currentGameId));
        wsClient.sendCommand(command);
    }

    private void sendLeave() {
        UserGameCommand command = new UserGameCommand(UserGameCommand.CommandType.LEAVE, authToken, Integer.parseInt(currentGameId));
        wsClient.sendCommand(command);
    }

    private ChessPosition parsePosition(String pos) throws Exception {
        if (pos.length() != 2) {
            throw new Exception("Invalid position format");
        }
        char colChar = pos.charAt(0);
        char rowChar = pos.charAt(1);
        int col = colChar - 'a' + 1;
        int row = rowChar - '0';
        if (col < 1 || col > 8 || row < 1 || row > 8) {
            throw new Exception("Position out of bounds");
        }
        return new ChessPosition(row, col);
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

    // Determinar si el tablero debe mostrarse desde la perspectiva de las blancas o las negras
    boolean isWhitePerspective = "white".equalsIgnoreCase(currentPlayerColor);

    // Definir el rango de filas y columnas según la perspectiva
    int startRow = isWhitePerspective ? 8 : 1;
    int endRow = isWhitePerspective ? 1 : 8;
    int rowIncrement = isWhitePerspective ? -1 : 1;
    int startCol = isWhitePerspective ? 1 : 8;
    int endCol = isWhitePerspective ? 8 : 1;
    int colIncrement = isWhitePerspective ? 1 : -1;

    // Mostrar el tablero
    for (int row = startRow; isWhitePerspective ? row >= endRow : row <= endRow; row += rowIncrement) {
        System.out.print(row + " ");
        for (int col = startCol; isWhitePerspective ? col <= endCol : col >= endCol; col += colIncrement) {
            ChessPosition pos = new ChessPosition(row, col);
            ChessPiece piece = board.getPiece(pos);
            String pieceSymbol = piece != null ? getPieceSymbol(piece) : EscapeSequences.EMPTY;
            String bgColor = ((row + col) % 2 == 0) ? EscapeSequences.SET_BG_COLOR_LIGHT_GREY : EscapeSequences.SET_BG_COLOR_DARK_GREY;
            System.out.print(bgColor + pieceSymbol + EscapeSequences.RESET_BG_COLOR);
        }
        System.out.println();
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