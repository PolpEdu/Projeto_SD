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

/**
 * SocketServer class
 * This class is responsible for the WebSocket server.
 * It is responsible for handling the WebSocket connections and messages.
 * It is also responsible for sending the top 10 searches to the clients.
 */
@Configuration
@EnableWebSocket
public class SocketServer implements WebSocketConfigurer {

    /**
     * The connected socket sessions.
     */
    private List<WebSocketSession> connectedSessions;

    /**
     * The RMI server interface. Used to get the top 10 searches.
     */
    private final RMIServerInterface sv;

    /**
     * Response of the Search Model to the top 10 searches request. Used to get the top 10 searches.
     */
    private ArrayList<String> top10Searches;

    /**
     * Constructor of the SocketServer class.
     * @param rmiServerInterface the RMI server (Search Model) interface
     */
    @Autowired
    SocketServer(RMIServerInterface rmiServerInterface) {
        this.sv = rmiServerInterface;
        this.connectedSessions = new ArrayList<>();
        this.top10Searches = new ArrayList<>();
    }

    /**
     * This function is called to register the WebSocket handlers. In the ip: ws://localhost:8080/topsocket
     * @param registry the WebSocket handler registry
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        try {
            registry.addHandler(myWebSocketHandler(sv), "/topsocket").setAllowedOrigins("*");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * This function is called to create a WebSocket handler. It is called by the registerWebSocketHandlers function.
     * @param sv the RMI server interface
     * @return the WebSocket handler
     * @throws RemoteException
     */
    @Bean
    public WebSocketHandler myWebSocketHandler(RMIServerInterface sv) throws RemoteException {
        return new MyWebSocketHandler(sv);
    }

    /**
     * This class is the WebSocket handler, it is called by the myWebSocketHandler function when a new WebSocket connection is established.
     */
    private class MyWebSocketHandler extends TextWebSocketHandler {
        /**
         * The RMI server interface.
         */
        private final RMIServerInterface sv;

        /**
         * Constructor of the MyWebSocketHandler class. It is called by the myWebSocketHandler function.
         * @param sv the RMI server interface
         */
        private MyWebSocketHandler(RMIServerInterface sv) {
            this.sv = sv;
        }

        /**
         * This function is called when a new WebSocket connection is established. It adds the new session to the connectedSessions list.
         * @param session the WebSocket session
         * @throws Exception if an error occurs
         */
        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            connectedSessions.add(session);
            System.out.println("New client connected: " + session.getId());
        }

        /**
         * This function is called when a new message is received from a client. It is not used in this project.
         * @param session the WebSocket session
         * @param message the message received
         * @throws IOException if an error occurs
         */
        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
            // Handle incoming messages from clients, if needed
        }

        /**
         * This function is called when a WebSocket connection is closed. It removes the session from the connectedSessions list.
         * @param session the WebSocket session
         * @param status the close status
         * @throws Exception
         */
        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
            connectedSessions.remove(session);
            System.out.println("Client connection closed: " + session.getId());
        }

        /**
         * This function is called every second to send the top 10 searches to the clients.
         * It iterates through the connectedSessions list and sends the top 10 searches to each client.
         * In a string format like: ["search1","search2","search3","search4","search5","search6","search7","search8","search9","search10"]
         * to be parsed by the client.
         * @throws RemoteException
         */
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

        /**
         * Helper function to convert a list of strings to a JSON string.
         * @param list the list of strings
         * @return the JSON string
         */
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