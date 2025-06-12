package ui;

import java.io.IOException;
import java.util.Scanner;
import chess.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import websocket.WebSocketClientManager;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.commands.ConnectCommand;
import websocket.messages.ServerMessage;
import websocket.messages.ServerMessageError;
import websocket.messages.ServerMessageNotification;
import websocket.messages.LoadGameMessage;

public class Client implements WebSocketClientManager.ClientMessageObserver {

    private final String serverURL;
    private final ServerFacade serverFacade;
    private WebSocketClientManager wsClient;
    public boolean isRunning;
    private boolean isLoggedIn;
    private ChessBoard currentBoard;
    private ChessGame.TeamColor currentPlayerColor;
    private String authToken;
    private Integer currentGameId;
    private boolean inGame;

    public Client(String serverURL) {
        this.serverURL = serverURL;
        this.serverFacade = new ServerFacade(serverURL);
        this.isRunning = true;
        this.isLoggedIn = false;
        this.currentBoard = new ChessBoard();
        this.currentBoard.resetBoard();
        this.currentPlayerColor = null;
        this.authToken = null;
        this.currentGameId = null;
        this.inGame = false;
    }

    public void run() {
        System.out.println("¡Bienvenido a 240 Chess! Escribe Help para empezar");
        Scanner scanner = new Scanner(System.in);
        String line;
        while (isRunning) {
            printPrompt();
            line = scanner.nextLine();
            try {
                handleCommand(line, scanner);
            } catch (Exception e) {
                System.out.println(EscapeSequences.SET_TEXT_COLOR_RED + "Error: " + e.getMessage() + EscapeSequences.RESET_TEXT_COLOR);
            }
        }
        scanner.close();
        try {
            if (wsClient != null) {
                wsClient.disconnect();
            }
        } catch (IOException e) {
            System.err.println("Error al desconectar WebSocket: " + e.getMessage());
        }
    }

    @Override
    public void onGameLoad(LoadGameMessage message) {
        System.out.println("\n--- ¡Juego Cargado! ---");
        this.currentBoard = message.getGame().getBoard();
        drawChessBoard(this.currentBoard, this.currentPlayerColor != null ? this.currentPlayerColor : ChessGame.TeamColor.WHITE);
        System.out.print("[IN GAME] Ingresa un comando (help, redraw, leave, make move, resign): ");
    }

    @Override
    public void onNotification(ServerMessageNotification message) {
        System.out.println("\n" + EscapeSequences.SET_TEXT_COLOR_CYAN + "--- Notificación del Servidor ---" + EscapeSequences.RESET_TEXT_COLOR);
        System.out.println(message.getMessage());
        printPrompt();
    }

    @Override
    public void onError(ServerMessageError message) {
        System.out.println("\n" + EscapeSequences.SET_TEXT_COLOR_RED + "--- ¡ERROR del Servidor! ---" + EscapeSequences.RESET_TEXT_COLOR);
        System.err.println(EscapeSequences.SET_TEXT_COLOR_RED + "Error: " + message.getErrorMessage() + EscapeSequences.RESET_TEXT_COLOR);
        printPrompt();
    }

    private void printPrompt() {
        if (inGame) {
            System.out.print(EscapeSequences.SET_TEXT_COLOR_BLUE + "[IN GAME] " + EscapeSequences.RESET_TEXT_COLOR + "Ingresa un comando (help, redraw, leave, make move, resign): ");
        } else if (isLoggedIn) {
            System.out.print(EscapeSequences.SET_TEXT_COLOR_GREEN + "[LOGGED IN] " + EscapeSequences.RESET_TEXT_COLOR + "Ingresa un comando (help, logout, create game, list games, join game, observe game): ");
        } else {
            System.out.print(EscapeSequences.SET_TEXT_COLOR_YELLOW + "[PRE-LOGIN] " + EscapeSequences.RESET_TEXT_COLOR + "Ingresa un comando (help, quit, login, register): ");
        }
    }

    private void handleCommand(String line, Scanner scanner) throws IOException, InvalidMoveException {
        String[] parts = line.trim().toLowerCase().split(" ", 2);
        String command = parts[0];
        String args = parts.length > 1 ? parts[1] : "";

        if (inGame) {
            handleInGameCommand(command, scanner, args);
        } else if (isLoggedIn) {
            handleLoggedInCommand(command, scanner, args);
        } else {
            handlePreLoginCommand(command, scanner, args);
        }
    }

    private void handlePreLoginCommand(String command, Scanner scanner, String args) throws IOException {
        switch (command) {
            case "help":
                System.out.println("Comandos disponibles: \n" +
                        "Help - Muestra este mensaje de ayuda\n" +
                        "Login - Inicia sesión en una cuenta existente\n" +
                        "Register - Registra un nuevo usuario\n" +
                        "Quit - Sale del cliente");
                break;
            case "login":
                login(scanner);
                break;
            case "register":
                register(scanner);
                break;
            case "quit":
                quit();
                break;
            default:
                System.out.println(EscapeSequences.SET_TEXT_COLOR_RED + "Comando desconocido. Escribe 'help' para ver los comandos disponibles." + EscapeSequences.RESET_TEXT_COLOR);
        }
    }

    private void handleLoggedInCommand(String command, Scanner scanner, String args) throws IOException {
        switch (command) {
            case "help":
                System.out.println("Comandos disponibles: \n" +
                        "Help - Muestra este mensaje de ayuda\n" +
                        "Logout - Cierra la sesión de la cuenta actual\n" +
                        "Create game - Crea un nuevo juego de ajedrez\n" +
                        "List games - Muestra una lista de todos los juegos disponibles\n" +
                        "Join game - Únete a un juego existente como jugador\n" +
                        "Observe game - Únete a un juego existente como observador");
                break;
            case "logout":
                logout();
                break;
            case "create":
                if (args.equals("game")) {
                    createGame(scanner);
                } else {
                    System.out.println(EscapeSequences.SET_TEXT_COLOR_RED + "Comando desconocido. ¿Quizás quisiste decir 'create game'?" + EscapeSequences.RESET_TEXT_COLOR);
                }
                break;
            case "list":
                if (args.equals("games")) {
                    listGames();
                } else {
                    System.out.println(EscapeSequences.SET_TEXT_COLOR_RED + "Comando desconocido. ¿Quizás quisiste decir 'list games'?" + EscapeSequences.RESET_TEXT_COLOR);
                }
                break;
            case "join":
                if (args.equals("game")) {
                    joinGame(scanner);
                } else {
                    System.out.println(EscapeSequences.SET_TEXT_COLOR_RED + "Comando desconocido. ¿Quizás quisiste decir 'join game'?" + EscapeSequences.RESET_TEXT_COLOR);
                }
                break;
            case "observe":
                if (args.equals("game")) {
                    observeGame(scanner);
                } else {
                    System.out.println(EscapeSequences.SET_TEXT_COLOR_RED + "Comando desconocido. ¿Quizás quisiste decir 'observe game'?" + EscapeSequences.RESET_TEXT_COLOR);
                }
                break;
            default:
                System.out.println(EscapeSequences.SET_TEXT_COLOR_RED + "Comando desconocido. Escribe 'help' para ver los comandos disponibles." + EscapeSequences.RESET_TEXT_COLOR);
        }
    }

    private void handleInGameCommand(String command, Scanner scanner, String args) throws IOException, InvalidMoveException {
        switch (command) {
            case "help":
                System.out.println("Comandos disponibles en juego: \n" +
                        "Help - Muestra este mensaje de ayuda\n" +
                        "Redraw - Vuelve a dibujar el tablero de ajedrez\n" +
                        "Leave - Abandona el juego actual\n" +
                        "Make move <start_pos> <end_pos> [promotion_piece] - Realiza un movimiento\n" +
                        "Resign - Renuncia al juego");
                break;
            case "redraw":
                drawChessBoard(currentBoard, currentPlayerColor != null ? currentPlayerColor : ChessGame.TeamColor.WHITE);
                break;
            case "leave":
                leaveGame();
                break;
            case "make":
                if (args.startsWith("move")) {
                    makeMove(scanner, args.substring(5).trim());
                } else {
                    System.out.println(EscapeSequences.SET_TEXT_COLOR_RED + "Uso: make move <posición_inicio> <posición_fin> [pieza_promoción]." + EscapeSequences.RESET_TEXT_COLOR);
                }
                break;
            case "resign":
                resignGame();
                break;
            default:
                System.out.println(EscapeSequences.SET_TEXT_COLOR_RED + "Comando desconocido en juego. Escribe 'help' para ver los comandos disponibles." + EscapeSequences.RESET_TEXT_COLOR);
        }
    }

    private void register(Scanner scanner) throws IOException {
        System.out.print("Ingresa tu nombre de usuario: ");
        String username = scanner.nextLine();
        System.out.print("Ingresa tu contraseña: ");
        String password = scanner.nextLine();
        System.out.print("Ingresa tu correo electrónico: ");
        String email = scanner.nextLine();

        String response = serverFacade.register(username, password, email);
        if (response.contains("Error")) {
            System.out.println(response);
        } else {
            Gson gson = new Gson();
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            this.authToken = jsonResponse.get("authToken").getAsString();
            this.isLoggedIn = true;
            System.out.println("¡Registro exitoso!");
        }
    }

    private void login(Scanner scanner) throws IOException {
        System.out.print("Ingresa tu nombre de usuario: ");
        String username = scanner.nextLine();
        System.out.print("Ingresa tu contraseña: ");
        String password = scanner.nextLine();

        String response = serverFacade.login(username, password);
        if (response.contains("Error")) {
            System.out.println(response);
        } else {
            Gson gson = new Gson();
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            this.authToken = jsonResponse.get("authToken").getAsString();
            this.isLoggedIn = true;
            System.out.println("¡Inicio de sesión exitoso!");
        }
    }

    private void logout() throws IOException {
        String response = serverFacade.logout(authToken);
        if (response.contains("Error")) {
            System.out.println(response);
        } else {
            this.authToken = null;
            this.isLoggedIn = false;
            System.out.println("¡Sesión cerrada exitosamente!");
        }
    }

    private void createGame(Scanner scanner) throws IOException {
        System.out.print("Ingresa el nombre del juego: ");
        String gameName = scanner.nextLine();

        String response = serverFacade.createGame(gameName, authToken);
        if (response.contains("Error")) {
            System.out.println(response);
        } else {
            Gson gson = new Gson();
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            String gameID = jsonResponse.get("gameID").getAsString();
            System.out.println("Juego '" + gameName + "' creado exitosamente con ID: " + gameID);
        }
    }

    private void listGames() throws IOException {
        String response = serverFacade.listGames(authToken);
        if (response.contains("Error")) {
            System.out.println(response);
        } else {
            Gson gson = new Gson();
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            JsonArray gamesArray = jsonResponse.getAsJsonArray("games");

            if (gamesArray == null || gamesArray.isEmpty()) {
                System.out.println("No hay juegos disponibles.");
                return;
            }

            System.out.println("--- Juegos Disponibles ---");
            int i = 1;
            for (JsonElement gameElement : gamesArray) {
                JsonObject game = gameElement.getAsJsonObject();
                String gameID = game.get("gameID").getAsString();
                String gameName = game.get("gameName").getAsString();
                String whiteUsername = game.has("whiteUsername") && !game.get("whiteUsername").isJsonNull() ? game.get("whiteUsername").getAsString() : "No asignado";
                String blackUsername = game.has("blackUsername") && !game.get("blackUsername").isJsonNull() ? game.get("blackUsername").getAsString() : "No asignado";

                System.out.printf("%d. ID: %s, Nombre: %s, Blanco: %s, Negro: %s%n",
                        i++, gameID, gameName, whiteUsername, blackUsername);
            }
        }
    }

    private void joinGame(Scanner scanner) throws IOException {
        System.out.print("Ingresa el ID del juego: ");
        String gameIDStr = scanner.nextLine();
        System.out.print("Ingresa el color del jugador (white/black, o deja vacío para observador): ");
        String playerColorStr = scanner.nextLine();

        try {
            Integer gameID = Integer.parseInt(gameIDStr);
            ChessGame.TeamColor playerColor = null;
            if (!playerColorStr.isEmpty()) {
                playerColor = ChessGame.TeamColor.valueOf(playerColorStr.toUpperCase());
            }

            serverFacade.joinGame(gameIDStr, playerColorStr, authToken);

            this.wsClient = new WebSocketClientManager(serverURL + "/ws", this); // Cambiado a /ws

            ConnectCommand connectCommand = new ConnectCommand(authToken, gameID, playerColor);
            wsClient.sendCommand(connectCommand);

            this.inGame = true;
            this.currentGameId = gameID;
            this.currentPlayerColor = playerColor;
            System.out.println("Te has unido al juego " + gameID + (playerColor != null ? " como " + playerColor.name().toLowerCase() : " como observador") + ".");
        } catch (NumberFormatException e) {
            System.out.println(EscapeSequences.SET_TEXT_COLOR_RED + "Error: El ID del juego debe ser un número." + EscapeSequences.RESET_TEXT_COLOR);
        } catch (IllegalArgumentException e) {
            System.out.println(EscapeSequences.SET_TEXT_COLOR_RED + "Error: El color del jugador debe ser 'white' o 'black'." + EscapeSequences.RESET_TEXT_COLOR);
        } catch (IOException e) {
            System.out.println(EscapeSequences.SET_TEXT_COLOR_RED + "Error del servidor al unirse al juego (HTTP): " + e.getMessage() + EscapeSequences.RESET_TEXT_COLOR);
        } catch (Exception e) {
            System.out.println(EscapeSequences.SET_TEXT_COLOR_RED + "No se pudo establecer la conexión WebSocket: " + e.getMessage() + EscapeSequences.RESET_TEXT_COLOR);
            this.inGame = false;
        }
    }

    private void observeGame(Scanner scanner) throws IOException {
        System.out.print("Ingresa el ID del juego para observar: ");
        String gameIDStr = scanner.nextLine();

        try {
            Integer gameID = Integer.parseInt(gameIDStr);

            serverFacade.joinGame(gameIDStr, null, authToken);

            this.wsClient = new WebSocketClientManager(serverURL + "/ws", this); // Cambiado a /ws

            ConnectCommand connectCommand = new ConnectCommand(authToken, gameID, null);
            wsClient.sendCommand(connectCommand);

            this.inGame = true;
            this.currentGameId = gameID;
            this.currentPlayerColor = null;
            System.out.println("Te has unido al juego " + gameID + " como observador.");
        } catch (NumberFormatException e) {
            System.out.println(EscapeSequences.SET_TEXT_COLOR_RED + "Error: El ID del juego debe ser un número." + EscapeSequences.RESET_TEXT_COLOR);
        } catch (IOException e) {
            System.out.println(EscapeSequences.SET_TEXT_COLOR_RED + "Error del servidor al observar el juego (HTTP): " + e.getMessage() + EscapeSequences.RESET_TEXT_COLOR);
        } catch (Exception e) {
            System.out.println(EscapeSequences.SET_TEXT_COLOR_RED + "No se pudo establecer la conexión WebSocket: " + e.getMessage() + EscapeSequences.RESET_TEXT_COLOR);
            this.inGame = false;
        }
    }

    private void makeMove(Scanner scanner, String moveArgs) throws IOException {
        String[] parts = moveArgs.split(" ");
        if (parts.length < 2) {
            System.out.println(EscapeSequences.SET_TEXT_COLOR_RED + "Uso: make move <posición_inicio> <posición_fin> [pieza_promoción]." + EscapeSequences.RESET_TEXT_COLOR);
            return;
        }

        ChessPosition startPos = parsePosition(parts[0]);
        ChessPosition endPos = parsePosition(parts[1]);
        ChessPiece.PieceType promotionPiece = null;
        if (parts.length > 2) {
            try {
                promotionPiece = ChessPiece.PieceType.valueOf(parts[2].toUpperCase());
                if (promotionPiece != ChessPiece.PieceType.QUEEN &&
                        promotionPiece != ChessPiece.PieceType.ROOK &&
                        promotionPiece != ChessPiece.PieceType.BISHOP &&
                        promotionPiece != ChessPiece.PieceType.KNIGHT) {
                    throw new IllegalArgumentException("Pieza de promoción inválida.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println(EscapeSequences.SET_TEXT_COLOR_RED + "Pieza de promoción inválida. Debe ser QUEEN, ROOK, BISHOP o KNIGHT." + EscapeSequences.RESET_TEXT_COLOR);
                return;
            }
        }

        if (startPos == null || endPos == null) {
            System.out.println(EscapeSequences.SET_TEXT_COLOR_RED + "Posición inválida. Usa el formato de ajedrez (ej. e2, e4)." + EscapeSequences.RESET_TEXT_COLOR);
            return;
        }

        ChessMove move = new ChessMove(startPos, endPos, promotionPiece);
        MakeMoveCommand makeMoveCommand = new MakeMoveCommand(authToken, currentGameId, move);
        wsClient.sendCommand(makeMoveCommand);
    }

    private void leaveGame() throws IOException {
        UserGameCommand leaveCommand = new UserGameCommand(UserGameCommand.CommandType.LEAVE, authToken, currentGameId);
        wsClient.sendCommand(leaveCommand);

        this.inGame = false;
        this.currentGameId = null;
        this.currentPlayerColor = null;
        this.currentBoard.resetBoard();

        System.out.println("Has abandonado el juego.");
    }

    private void resignGame() throws IOException {
        System.out.print("¿Estás seguro de que quieres renunciar al juego? (yes/no): ");
        Scanner confirmationScanner = new Scanner(System.in);
        String confirmation = confirmationScanner.nextLine().trim().toLowerCase();

        if (!confirmation.equals("yes")) {
            System.out.println("Renuncia cancelada.");
            return;
        }
        UserGameCommand resignCommand = new UserGameCommand(UserGameCommand.CommandType.RESIGN, authToken, currentGameId);
        wsClient.sendCommand(resignCommand);

        System.out.println("Solicitud de renuncia enviada. Esperando confirmación del servidor...");
    }

    private void quit() {
        System.out.println("Saliendo del cliente...");
        this.isRunning = false;
    }

    private void drawChessBoard(ChessBoard board, ChessGame.TeamColor playerPerspective) {
        System.out.print(EscapeSequences.ERASE_SCREEN);
        System.out.print(EscapeSequences.SET_CURSOR_TO_HOME_POSITION);
        System.out.print(EscapeSequences.RESET_ALL);

        boolean isWhitePerspective = (playerPerspective == ChessGame.TeamColor.WHITE);

        int startRow = isWhitePerspective ? 8 : 1;
        int endRow = isWhitePerspective ? 1 : 8;
        int rowIncrement = isWhitePerspective ? -1 : 1;

        int startCol = isWhitePerspective ? 1 : 8;
        int endCol = isWhitePerspective ? 8 : 1;
        int colIncrement = isWhitePerspective ? 1 : -1;

        System.out.print(EscapeSequences.SET_BG_COLOR_DARK_GREY + EscapeSequences.SET_TEXT_COLOR_WHITE + "   ");
        for (int col = startCol; isWhitePerspective ? col <= endCol : col >= endCol; col += colIncrement) {
            System.out.print(" " + (char) ('a' + col - 1) + " ");
        }
        System.out.println(EscapeSequences.RESET_ALL);

        for (int r = startRow; isWhitePerspective ? r >= endRow : r <= endRow; r += rowIncrement) {
            System.out.print(EscapeSequences.SET_BG_COLOR_DARK_GREY + EscapeSequences.SET_TEXT_COLOR_WHITE + " " + r + " " + EscapeSequences.RESET_ALL);

            for (int c = startCol; isWhitePerspective ? c <= endCol : c >= endCol; c += colIncrement) {
                ChessPosition position = new ChessPosition(r, c);
                ChessPiece piece = board.getPiece(position);

                boolean isLightSquare = (r + c) % 2 != 0;
                String bgColor = isLightSquare ? EscapeSequences.SET_BG_COLOR_LIGHT_GREY : EscapeSequences.SET_BG_COLOR_BRIGHT_GREEN;

                System.out.print(bgColor);
                String pieceSymbol = getPieceSymbol(piece);
                System.out.print(pieceSymbol);
            }
            System.out.println(EscapeSequences.RESET_ALL);
        }

        System.out.print(EscapeSequences.SET_BG_COLOR_DARK_GREY + EscapeSequences.SET_TEXT_COLOR_WHITE + "   ");
        for (int col = startCol; isWhitePerspective ? col <= endCol : col >= endCol; col += colIncrement) {
            System.out.print(" " + (char) ('a' + col - 1) + " ");
        }
        System.out.println(EscapeSequences.RESET_ALL);
        System.out.print(EscapeSequences.RESET_ALL);
    }

    private String getPieceSymbol(ChessPiece piece) {
        if (piece == null) {
            return EscapeSequences.EMPTY;
        }

        ChessGame.TeamColor color = piece.getTeamColor();
        ChessPiece.PieceType type = piece.getPieceType();

        String pieceColorCode = (color == ChessGame.TeamColor.WHITE) ? EscapeSequences.SET_TEXT_COLOR_WHITE : EscapeSequences.SET_TEXT_COLOR_BLACK;

        String symbol;
        switch (type) {
            case PAWN: symbol = (color == ChessGame.TeamColor.WHITE) ? EscapeSequences.WHITE_PAWN : EscapeSequences.BLACK_PAWN; break;
            case KNIGHT: symbol = (color == ChessGame.TeamColor.WHITE) ? EscapeSequences.WHITE_KNIGHT : EscapeSequences.BLACK_KNIGHT; break;
            case BISHOP: symbol = (color == ChessGame.TeamColor.WHITE) ? EscapeSequences.WHITE_BISHOP : EscapeSequences.BLACK_BISHOP; break;
            case ROOK: symbol = (color == ChessGame.TeamColor.WHITE) ? EscapeSequences.WHITE_ROOK : EscapeSequences.BLACK_ROOK; break;
            case QUEEN: symbol = (color == ChessGame.TeamColor.WHITE) ? EscapeSequences.WHITE_QUEEN : EscapeSequences.BLACK_QUEEN; break;
            case KING: symbol = (color == ChessGame.TeamColor.WHITE) ? EscapeSequences.WHITE_KING : EscapeSequences.BLACK_KING; break;
            default: symbol = EscapeSequences.EMPTY;
        }
        return pieceColorCode + symbol + EscapeSequences.RESET_TEXT_COLOR;
    }

    private ChessPosition parsePosition(String posStr) {
        if (posStr == null || posStr.length() != 2) {
            return null;
        }
        char colChar = Character.toLowerCase(posStr.charAt(0));
        int row = Character.getNumericValue(posStr.charAt(1));

        if (colChar < 'a' || colChar > 'h' || row < 1 || row > 8) {
            return null;
        }

        int col = colChar - 'a' + 1;
        return new ChessPosition(row, col);
    }

    public static void main(String[] args) {
        String serverURL = "http://localhost:8080";
        Client client = new Client(serverURL);
        client.run();
    }
}