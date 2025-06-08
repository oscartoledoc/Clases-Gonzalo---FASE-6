package websocket;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.util.function.Consumer;


@WebSocket
public class WebSocketClientEndpoint {
    private Session session;
    private Consumer<String> messageHandler;

    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    @OnWebSocketConnect
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("Connected to WebSocket");
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        if (messageHandler != null) {
            messageHandler.accept(message);
        }
    }

    public void sendMessage(String message) throws Exception {
        if (session!= null && session.isOpen()) {
            session.getRemote().sendString(message);
        } else {
            throw new IllegalStateException("Session not open");
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("Disconnected: " + reason);
        this.session = null;
    }

    public boolean isOpen() {
        return session != null && session.isOpen();
    }

}
