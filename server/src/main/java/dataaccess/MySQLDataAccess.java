package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MySQLDataAccess implements DataAccess {
    private final Gson gson = new Gson();
    // ELIMINADO: private final AtomicInteger gameIdCounter = new AtomicInteger(1);

    public MySQLDataAccess() throws DataAccessException {
        DatabaseManager.createDatabase();
        try (Connection conn = DatabaseManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (\n" +
                    "username VARCHAR (255) PRIMARY KEY, \n" +
                    "password VARCHAR (255) NOT NULL, \n" +
                    "email VARCHAR (255) NOT NULL\n" +
                    ");");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS auth (\n" +
                    "authToken VARCHAR (255) PRIMARY KEY, \n" +
                    "username VARCHAR (255) NOT NULL,\n" +
                    "FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE\n" +
                    ");");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS games (\n" +
                    "gameID INT PRIMARY KEY AUTO_INCREMENT,\n" + // AUTO_INCREMENT es clave aquí
                    "whiteUsername VARCHAR (255),\n" +
                    "blackUsername VARCHAR (255),\n" +
                    "gameName VARCHAR (255) NOT NULL,\n" +
                    "game TEXT,\n" +
                    "FOREIGN KEY (whiteUsername) REFERENCES users(username) ON DELETE SET NULL,\n" +
                    "FOREIGN KEY (blackUsername) REFERENCES users(username) ON DELETE SET NULL\n" +
                    ");");
        } catch (SQLException e) {
            throw new DataAccessException("failed to create tables " + e.getMessage());
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement stmt = conn.prepareStatement
                ("SELECT * FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new UserData
                        (rs.getString("username"),
                                rs.getString("password"),
                                rs.getString("email"));
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("failed to get user " + e.getMessage());
        }
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement stmt = conn.prepareStatement
                ("INSERT INTO users(username, password, email) VALUES (?, ?, ?)") ) {
            String hashedPassword = BCrypt.hashpw(user.password(), BCrypt.gensalt(12));
            stmt.setString(1, user.username());
            stmt.setString(2, hashedPassword);
            stmt.setString(3, user.email());
            stmt.executeUpdate();
        } catch (SQLException e){
            throw new DataAccessException("failed to create user " + e.getMessage());
        }
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement stmt = conn.prepareStatement
                ("SELECT * FROM auth WHERE authToken = ?")) {
            stmt.setString(1, authToken);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new AuthData(rs.getString("username"), rs.getString("authToken"));
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("failed to get auth " + e.getMessage());
        }
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement stmt = conn.prepareStatement
                ("INSERT INTO auth(authToken, username) VALUES (?, ?)") ) {
            stmt.setString(1, auth.authToken());
            stmt.setString(2, auth.username());
            stmt.executeUpdate();
        } catch (SQLException e){
            throw new DataAccessException("failed to create auth " + e.getMessage());
        }
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException{
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement stmt = conn.prepareStatement
                ("DELETE FROM auth WHERE authToken = ?")) {
            stmt.setString(1, authToken);
            stmt.executeUpdate();
        } catch (SQLException e){
            throw new DataAccessException("failed to delete auth " + e.getMessage());
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException{
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement stmt = conn.prepareStatement
                ("SELECT * FROM games WHERE gameID = ?")){
            stmt.setInt(1, gameID);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()){
                String gameJson = rs.getString("game");
                ChessGame game = gameJson != null ? gson.fromJson(gameJson, ChessGame.class) : null;
                return new GameData
                        (rs.getInt("gameID"),
                                rs.getString("whiteUsername"),
                                rs.getString("blackUsername"),
                                rs.getString("gameName"), game);
            }
            return null;
        } catch (SQLException e){
            throw new DataAccessException("failed to get game " + e.getMessage());
        }
    }

    @Override
    public int createGame(GameData game) throws DataAccessException {
        // MODIFICADO: NO incluyas gameID en el INSERT, deja que AUTO_INCREMENT lo genere
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO games(whiteUsername, blackUsername, gameName, game) VALUES (?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) { // Importante para recuperar el ID
            stmt.setString(1, game.whiteUsername());
            stmt.setString(2, game.blackUsername());
            stmt.setString(3, game.gameName());
            stmt.setString(4, game.game() != null ? gson.toJson(game.game()) : null);
            stmt.executeUpdate();

            // Recuperar el gameID generado por la base de datos
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1); // Devuelve el ID generado
                } else {
                    throw new DataAccessException("Failed to get generated game ID.");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("failed to create game: " + e.getMessage());
        }
    }

    @Override
    public void updateGame(int gameID, GameData game) throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement stmt = conn.prepareStatement
                ("UPDATE games SET whiteUsername = ?, blackUsername = ?, gameName = ?, game = ? WHERE gameID = ?")) {
            stmt.setString(1, game.whiteUsername());
            stmt.setString(2, game.blackUsername());
            stmt.setString(3, game.gameName());
            stmt.setString(4, game.game() != null ? gson.toJson(game.game()) : null);
            stmt.setInt(5, gameID);
            int rowAffected = stmt.executeUpdate();
            if (rowAffected == 0) {
                throw new DataAccessException("failed to update game");
            }
        } catch (SQLException e) {
            throw new DataAccessException("failed to update game " + e.getMessage());
        }
    }

    @Override
    public GameData[] getAllGames() throws DataAccessException{
        List<GameData> games = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement stmt = conn.prepareStatement
                ("SELECT * FROM games")){
            ResultSet rs = stmt.executeQuery();
            while (rs.next()){
                String gameJson = rs.getString("game");
                ChessGame game = gameJson != null ? gson.fromJson(gameJson, ChessGame.class) : null;
                games.add(new GameData
                        (rs.getInt("gameID"),
                                rs.getString("whiteUsername"),
                                rs.getString("blackUsername"),
                                rs.getString("gameName"), game));
            }
            return games.toArray(new GameData[0]);
        } catch (SQLException e){
            throw new DataAccessException("failed to get all games " + e.getMessage());
        }
    }

    // ELIMINADO: Este método ya no es necesario ni se implementa, ya que la DB genera el ID.
    // @Override
    // public int generateGameID() throws DataAccessException {
    //     throw new DataAccessException("generateGameID() no debe ser llamado; el ID es AUTO_INCREMENT.");
    // }

    @Override
    public void clear() throws DataAccessException{
        deleteAllGames();
        deleteAllAuth();
        deleteAllUsers();
        // REINICIAR EL CONTADOR DE AUTO_INCREMENT DESPUÉS DE ELIMINAR LOS JUEGOS
        try (Connection conn = DatabaseManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE games AUTO_INCREMENT = 1");
        } catch (SQLException e) {
            throw new DataAccessException("Failed to reset AUTO_INCREMENT for games table: " + e.getMessage());
        }
    }

    @Override
    public void deleteAllUsers() throws DataAccessException{
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement stmt = conn.prepareStatement("DELETE FROM users")){
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("failed to delete all users " + e.getMessage());
        }
    }

    @Override
    public void deleteAllAuth() throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement stmt = conn.prepareStatement("DELETE FROM auth")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("failed to delete all auths " + e.getMessage());
        }
    }

    @Override
    public void deleteAllGames() throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement stmt = conn.prepareStatement("DELETE FROM games")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("failed to delete all games " + e.getMessage());
        }
    }

    @Override
    public boolean isObserver(int gameID, String username) throws DataAccessException {

        GameData game = getGame(gameID);
        if (game == null) {
            return false;
        }

        if (Objects.equals(game.whiteUsername(), username) || Objects.equals(game.blackUsername(), username)) {
            return false;
        }

        UserData user = getUser(username);
        return user != null;
    }
}