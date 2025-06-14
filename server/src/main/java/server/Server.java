package server;

import dataaccess.DataAccessException;
import dataaccess.DataAccess;
import dataaccess.MySQLDataAccess;
import model.AuthData;
import model.GameData;
import model.UserData;
import service.*;
import com.google.gson.Gson;
import spark.*;
import service.Results.*;
import websocket.WebSocketServer;

public class Server {

    private final DataAccess dataaccess;
    private final UserService userService;
    private final GameService gameService;
    private final SessionService sessionService;
    private final ClearService clearService;
    private final Gson gson;
    private final WebSocketServer webSocketServer;

    public Server() {
        try {
            this.dataaccess = new MySQLDataAccess();
            this.userService = new UserService(dataaccess);
            this.gameService = new GameService(dataaccess);
            this.sessionService = new SessionService(dataaccess);
            this.clearService = new ClearService(dataaccess);
            this.webSocketServer = new WebSocketServer(gameService);
            this.gson = new Gson();
        } catch (DataAccessException e) {
            // Es vital imprimir la excepción aquí para depurar problemas de DB
            System.err.println("ERROR: Fallo al inicializar el servidor debido a un problema de base de datos.");
            e.printStackTrace(); // Esto imprimirá el stack trace completo del error de la base de datos
            throw new RuntimeException("Unable to connect to database", e);
        }
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);

        Spark.webSocket("/ws", webSocketServer);

        Spark.staticFiles.location("web");

        registerEndpoints();

        Spark.init();

        Spark.awaitInitialization();
        System.out.println("Servidor Spark iniciado en el puerto: " + Spark.port()); // Mensaje de confirmación
        return Spark.port();
    }

    private void registerEndpoints() {
        // ... (Tu código actual de registerEndpoints va aquí, sin cambios)
        Spark.post("/user", (req, res) -> {
            try {
                RegisterRequest request = gson.fromJson(req.body(), RegisterRequest.class);
                if (request == null || request.username == null || request.password == null || request.email == null) {
                    res.status(400);
                    return gson.toJson(new ErrorResponse("Error: Bad request"));
                }
                RegisterResult result = userService.register(request.username, request.password, request.email);
                res.status(200);
                return gson.toJson(result);
            } catch (DataAccessException e) {
                if (e.getMessage().equals("Username already exists")) {
                    res.status(403);
                    return gson.toJson(new ErrorResponse("Error: Already exists"));
                }
                res.status(500);
                return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
            }
        });
        Spark.post("/session", (req, res) -> {
            try {
                LoginRequest request = gson.fromJson(req.body(), LoginRequest.class);
                if (request == null || request.username == null || request.password == null) {
                    res.status(400);
                    return gson.toJson(new ErrorResponse("Error: Bad Request"));
                }
                RegisterResult result = userService.login(request.username, request.password);
                res.status(200);
                return gson.toJson(result);
            } catch (DataAccessException e) {
                if (e.getMessage().equals("Invalid Credentials")) {
                    res.status(401);
                    return gson.toJson(new ErrorResponse("Error: Unauthorized"));
                }
                res.status(500);
                return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
            }
        });
        Spark.delete("/session", (req, res) -> {
            String authToken = req.headers("Authorization");
            if (authToken == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Error: Unauthorized"));
            }
            try {
                if (dataaccess.getAuth(authToken) == null) {
                    res.status(401);
                    return gson.toJson(new ErrorResponse("Error: Unauthorized"));
                }
                Result result = sessionService.logout(authToken);
                res.status(200);
                return gson.toJson(new EmptyResponse());
            } catch (DataAccessException e) {
                res.status(500);
                return gson.toJson(new ErrorResponse("Error: Bad Request"));
            }
        });
        Spark.get("/game", (req, res) -> {
            String authToken = req.headers("Authorization");
            if (authToken == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Error: Unauthorized"));
            }
            try {
                GameListResult result = gameService.listGames(authToken);
                res.status(200);
                return gson.toJson(new GameListResponse(result.games()));
            } catch (DataAccessException e) {
                if (e.getMessage().equals("Unauthorized")) {
                    res.status(401);
                    return gson.toJson(new ErrorResponse("Error: Unauthorized"));
                }
                res.status(500);
                return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
            }
        });
        Spark.post("/game", (req, res) -> {
            String authToken = req.headers("Authorization");
            if (authToken == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Error: Unauthorized"));
            }
            try {
                AuthData auth = dataaccess.getAuth(authToken);
                if (auth == null) {
                    res.status(401);
                    return gson.toJson(new ErrorResponse("Error: Unauthorized"));
                }
                UserData user = dataaccess.getUser(auth.username());
                if (user == null) {
                    res.status(401);
                    return gson.toJson(new ErrorResponse("Error: Unauthorized"));
                }
                if (req.body() == null || req.body().trim().isEmpty()) {
                    res.status(401);
                    return gson.toJson(new ErrorResponse("Error: Unauthorized"));
                }
                CreateGameRequest request = gson.fromJson(req.body(), CreateGameRequest.class);
                if (request == null || request.gameName == null) {
                    res.status(400);
                    return gson.toJson(new ErrorResponse("Error: Bad request"));
                }
                CreateGameResult result = gameService.createGame(authToken, request.gameName);
                res.status(200);
                return gson.toJson(result);
            } catch (DataAccessException e) {
                if (e.getMessage().equals("Unauthorized")) {
                    res.status(401);
                    return gson.toJson(new ErrorResponse("Error: Unauthorized"));
                }
                res.status(500);
                return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
            } catch (Exception e) {
                res.status(500);
                return gson.toJson(new ErrorResponse("Error: Bad request"));
            }
        });
        Spark.put("/game", (req, res) -> {
            String authToken = req.headers("Authorization");
            if (authToken == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Error: Unauthorized"));
            }
            try {
                JoinGameRequest request = gson.fromJson(req.body(), JoinGameRequest.class);
                if (request == null || request.gameID == null || request.playerColor == null) {
                    res.status(400);
                    return gson.toJson(new ErrorResponse("Error: Bad request"));
                }
                JoinGameResult result = gameService.joinGame(authToken, request.gameID, request.playerColor);
                res.status(200);
                return gson.toJson(new EmptyResponse());
            } catch (DataAccessException e) {
                if (e.getMessage().equals("Unauthorized")) {
                    res.status(401);
                    return gson.toJson(new ErrorResponse("Error: Unauthorized"));
                } else if (e.getMessage().equals("Invalid Game") || e.getMessage().equals("Invalid Color")) {
                    res.status(400);
                    return gson.toJson(new ErrorResponse("Error: Bad request"));
                } else if (e.getMessage().equals("Player already joined")) {
                    res.status(403);
                    return gson.toJson(new ErrorResponse("Error: Player already joined"));
                }
                res.status(500);
                return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
            }
        });
        Spark.post("/clear", (req, res) -> {
            try {
                Result result = clearService.clear();
                res.status(200);
                return gson.toJson(new EmptyResponse());
            } catch (DataAccessException e) {
                res.status(500);
                return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
            }
        });
        Spark.delete("/db", (req, res) -> {
            try {
                Result result = clearService.clear();
                res.status(200);
                return gson.toJson(new EmptyResponse());
            } catch (DataAccessException e) {
                res.status(500);
                return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
            }
        });
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
        webSocketServer.stop();
    }

    // AÑADE ESTE MÉTODO MAIN AL FINAL DE TU CLASE Server.java
    public static void main(String[] args) {
        var server = new Server();
        // Puedes cambiar el puerto si lo necesitas, pero 8080 es estándar para la aplicación cliente
        server.run(8080);
    }

    private record RegisterRequest(String username, String password, String email) {}

    private record LoginRequest(String username, String password) {}

    private record CreateGameRequest(String gameName) {}

    private record JoinGameRequest(Integer gameID, String playerColor) {}

    private record GameListResponse(GameData[] games) {}

    private record ErrorResponse(String message) {}

    private record EmptyResponse() {}
}