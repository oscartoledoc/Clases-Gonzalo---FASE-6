package service;

import model.GameData;

public class Results {

    public record RegisterResult(String username, String authToken) {}

    public record GameListResult(GameData[] games) {}

    public record CreateGameResult(int gameID) {}

    public record Result(String message) {}

    public record JoinGameResult(String authToken, int gameID, String playerColor) {}
}
