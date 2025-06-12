import chess.*;
import ui.Client;

public class Main {
    public static void main(String[] args) {
        String serverURL = "http://localhost:8080";
        Client client = new Client(serverURL);
        client.run();
    }
}