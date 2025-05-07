package model;

import chess.ChessGame;

public record UserData(String username, String password, String email) {

}

public record GameData(int gameID, String gameName, String whiteUsername, String blackUsername,  ChessGame game) {

}

public record AuthData(String username, String authToken) {

}