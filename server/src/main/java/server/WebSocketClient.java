//package server;
//
//import com.google.gson.Gson;
//import com.mysql.cj.xdevapi.Client;
//import org.eclipse.jetty.websocket.api.Session;
//import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
//import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
//import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
//
//import java.io.IOException;
//import java.net.URI;
//
//import static websocket.commands.UserGameCommand.CommandType.*;
//
//public class WebSocketClient {
//    private Session session;
//    private Client client;
//    private final Gson gson = new Gson();
//
//    public WebSocketClient(Session session) {
//        this.client = client;
//    }
//
//
//    public void connect(String url, String authToken, String gameId) throws Exception {
//        WebSocketClient wsClient = new WebSocketClient();
//        wsClient.start();
//        URI uri = new URI(url + "?authToken=" + authToken + "&gameId=" + gameId);
//        wsClient.connect(this, uri);
//    }
//
//    @OnWebSocketConnect
//    public void onConnect(Session session) {
//        this.session = session;
//        System.out.println("Connected to WebSocket");
//    }
//
//    @OnWebSocketMessage
//    public void onMessage(String message) {
//        System.out.println("Received: " + message);
//        // Actualizar tablero manualmente si es necesario
//        client.displayBoard();
//    }
//
//    @OnWebSocketClose
//    public void onClose(int statusCode, String reason) {
//        System.out.println("Disconnected: " + reason);
//        this.session = null;
//    }
//
//    public void sendMove(String moveJson) throws IOException {
//        if (session != null && session.isOpen()) {
//            session.getRemote().sendString("{\"commandType\":\"MAKE_MOVE\",\"authToken\":\"" + client.authToken + "\",\"gameID\":" + client.currentGameId + ",\"move\":" + moveJson + "}");
//        }
//    }
//
//    public void sendResign() throws IOException {
//        if (session != null && session.isOpen()) {
//            session.getRemote().sendString("{\"commandType\":\"RESIGN\",\"authToken\":\"" + client.authToken + "\",\"gameID\":" + client.currentGameId + "}");
//        }
//    }
//
//    public void sendLeave() throws IOException {
//        if (session != null && session.isOpen()) {
//            session.getRemote().sendString("{\"commandType\":\"LEAVE\",\"authToken\":\"" + client.authToken + "\",\"gameID\":" + client.currentGameId + "}");
//        }
//
//
//
//    }
//}
