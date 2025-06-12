package websocket;

import chess.ChessGame;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import websocket.commands.UserGameCommand;
import websocket.commands.ConnectCommand;
import websocket.messages.ServerMessage;
import websocket.messages.ServerMessageError;
import websocket.messages.ServerMessageNotification;
import websocket.messages.LoadGameMessage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@WebSocket
public class WebSocketClientManager {
    private Session session;
    private ClientMessageObserver observer;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public interface ClientMessageObserver {
        void onGameLoad(LoadGameMessage message);
        void onNotification(ServerMessageNotification message);
        void onError(ServerMessageError message);
    }

    public WebSocketClientManager(String serverUrl, ClientMessageObserver observer) {
        this.observer = observer;
        try {
            String wsUrl = serverUrl.replace("http", "ws") + "/ws";
            URI uri = new URI(wsUrl);

            org.eclipse.jetty.websocket.client.WebSocketClient client = new org.eclipse.jetty.websocket.client.WebSocketClient();
            client.start();

            Future<Session> fut = client.connect(this, uri);
            this.session = fut.get(5, TimeUnit.SECONDS);

            System.out.println("WebSocket connection established to: " + wsUrl);

        } catch (URISyntaxException e) {
            System.err.println("Error de sintaxis de URI al conectar WebSocket: " + e.getMessage());
            if (observer != null) observer.onError(new ServerMessageError("Error de URI: " + e.getMessage()));
        } catch (IOException e) {
            System.err.println("Error de IO al conectar WebSocket: " + e.getMessage());
            if (observer != null) observer.onError(new ServerMessageError("Error de IO: " + e.getMessage()));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.err.println("Error al establecer la conexión WebSocket: " + e.getMessage());
            System.err.println("Por favor, asegúrate de que el servidor está corriendo en " + serverUrl + " y su endpoint WebSocket es /ws");
            if (observer != null) observer.onError(new ServerMessageError("Fallo al conectar al servidor WebSocket: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error inesperado al conectar WebSocket: " + e.getMessage());
            e.printStackTrace();
            if (observer != null) observer.onError(new ServerMessageError("Error inesperado: " + e.getMessage()));
        }
    }

    public void sendCommand(UserGameCommand command) throws IOException {
        if (session != null && session.isOpen()) {
            session.getRemote().sendString(gson.toJson(command));
        } else {
            throw new IOException("WebSocket session no está abierta.");
        }
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        try {
            ServerMessage baseMessage = gson.fromJson(message, ServerMessage.class);
            if (observer != null) {
                switch (baseMessage.getServerMessageType()) {
                    case LOAD_GAME:
                        LoadGameMessage loadMessage = gson.fromJson(message, LoadGameMessage.class);
                        observer.onGameLoad(loadMessage);
                        break;
                    case NOTIFICATION:
                        ServerMessageNotification notificationMessage = gson.fromJson(message, ServerMessageNotification.class);
                        observer.onNotification(notificationMessage);
                        break;
                    case ERROR:
                        ServerMessageError errorMessage = gson.fromJson(message, ServerMessageError.class);
                        observer.onError(errorMessage);
                        break;
                    default:
                        System.err.println("Received unknown server message type: " + baseMessage.getServerMessageType());
                }
            }
        } catch (Exception e) {
            System.err.println("Error al procesar mensaje WebSocket: " + e.getMessage());
            e.printStackTrace();
            if (observer != null) {
                observer.onError(new ServerMessageError("Error al procesar mensaje del servidor: " + e.getMessage()));
            }
        }
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.println("WebSocket closed: StatusCode=" + statusCode + ", Reason=" + reason);
        if (observer != null) {
            observer.onNotification(new ServerMessageNotification("WebSocket connection closed. Reason: " + reason));
        }
    }

    @OnWebSocketError
    public void onError(Throwable cause) {
        System.err.println("WebSocket error: " + cause.getMessage());
        cause.printStackTrace();
        if (observer != null) {
            observer.onError(new ServerMessageError("WebSocket error: " + cause.getMessage()));
        }
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("WebSocket connected successfully: " + session.getRemoteAddress());
        this.session = session;
    }

    public void disconnect() throws IOException {
        if (session != null && session.isOpen()) {
            session.close();
            System.out.println("WebSocket disconnected.");
        }
    }
}