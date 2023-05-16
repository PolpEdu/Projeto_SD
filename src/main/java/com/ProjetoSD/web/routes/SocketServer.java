package com.ProjetoSD.web.routes;

import com.ProjetoSD.interfaces.RMIServerInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSocket
public class SocketServer implements WebSocketConfigurer {

    private List<WebSocketSession> connectedSessions;
    private final RMIServerInterface sv;
    private ArrayList<String> top10Searches;

    @Autowired
    SocketServer(RMIServerInterface rmiServerInterface) {
        this.sv = rmiServerInterface;
        this.connectedSessions = new ArrayList<>();
        this.top10Searches = new ArrayList<>();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        try {
            registry.addHandler(myWebSocketHandler(sv), "/topsocket").setAllowedOrigins("*");
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }
    }

    @Bean
    public WebSocketHandler myWebSocketHandler(RMIServerInterface sv) throws RemoteException {
        return new MyWebSocketHandler(sv);
    }

    private class MyWebSocketHandler extends TextWebSocketHandler {
        private final RMIServerInterface sv;

        private MyWebSocketHandler(RMIServerInterface sv) {
            this.sv = sv;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            connectedSessions.add(session);
            System.out.println("New client connected: " + session.getId());
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
            // Handle incoming messages from clients, if needed
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
            connectedSessions.remove(session);
            System.out.println("Client connection closed: " + session.getId());
        }

        @Scheduled(fixedRate = 1000)
        public void sendTop10SearchesToClients() throws RemoteException {
            while (true) {
                top10Searches = this.sv.getTop10Searches();

                for (WebSocketSession session : connectedSessions) {
                    if (session.isOpen()) {
                        try {
                            System.out.println("Sending top 10 searches to client: " + session.getId());
                            String json = createJsonFromList(top10Searches); // Convert list to JSON string
                            session.sendMessage(new TextMessage(json));
                            Thread.sleep(200);
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        private String createJsonFromList(ArrayList<String> list) {
            StringBuilder json = new StringBuilder();
            json.append("[");
            for (int i = 0; i < list.size(); i++) {
                json.append("\"").append(list.get(i)).append("\"");
                if (i != list.size() - 1) {
                    json.append(",");
                }
            }
            json.append("]");
            return json.toString();
        }
    }
}