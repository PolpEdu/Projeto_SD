package Utility;

import Client.User;
import SearchEngine.Database;
import SearchEngine.Downloader;
import SearchEngine.TCPServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.LinkedList;

// Class used to send and receive messages from Multicast Server
public class Request extends Thread {
    private int msgSize;
    private String msg;
    private LinkedList<Message> msgQueue; // Receive Messages Queue

    private int MCAST_SEND_PORT;
    private int MCAST_RECEIVE_PORT;
    private InetAddress group; // ip address to send and receive messages

    private MulticastSocket receiveSocket;
    private MulticastSocket sendSocket;

    int sv_port; // server id

    Database db;
    Downloader downloader;

    String tcpHost;
    TCPServer tcpServer;

    Connection connection;

    public Request(int msgSize, String msg, LinkedList<Message> msgQueue, int MCAST_SEND_PORT, int MCAST_RECEIVE_PORT, InetAddress group, MulticastSocket receiveSocket, MulticastSocket sendSocket, int sv_id, Database db, Downloader downloader, String tcpHost, TCPServer tcpServer, Connection con) {
        this.msgSize = msgSize;
        this.msg = msg;
        this.msgQueue = msgQueue;
        this.MCAST_SEND_PORT = MCAST_SEND_PORT;
        this.MCAST_RECEIVE_PORT = MCAST_RECEIVE_PORT;
        this.group = group;
        this.receiveSocket = receiveSocket;
        this.sendSocket = sendSocket;
        this.sv_port = sv_id;
        this.db = db;
        this.downloader = downloader;
        this.tcpHost = tcpHost;
        this.tcpServer = tcpServer;

        this.connection = con;
    }

    public void run() {
        String[] msgSplit = this.msg.split("\\|");
        DatagramPacket sendPacket = null;
        Message msg = null;
        try {
            // messages will be always composed by 3 parts initially: id|type:<type>|status:<status>|...
            String id = msgSplit[0].split(":")[1];
            String type = msgSplit[1].split(":")[1];
            String status = msgSplit[2].split(":")[1];


            if (type.equals("alive")) {

                String address = msgSplit[3].split(":")[1];
                int port = Integer.parseInt(msgSplit[4].split(":")[1]);

                if (status.equals("ack")) {
                    // ack: we are the destination, update the ports accordingly.
                    // ACK(type: alive, status: ack, address: <address>, port: <port>)
                    String destAddr = msgSplit[5].split(":")[1];
                    int destPort = Integer.parseInt(msgSplit[6].split(":")[1]);

                    if (destAddr.equals(this.tcpHost) && destPort == this.sv_port) {
                        // we are the destination, update the ports accordingly
                        this.connection.updatePorts(address, port);
                    }
                    return;
                }


                // check if the message is not for this server
                if (!(address.equals(this.tcpHost) && port == this.sv_port)) {
                    String send = parseMsg(this.msg);
                    msg = new Message(send, id);
                    byte[] sendbuffer = msg.message.getBytes();
                    sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length, this.group, this.MCAST_RECEIVE_PORT);
                    sendSocket.send(sendPacket);
                }
            } else {
                // if the message is not type alive, it is a request
                String send = parseMsg(this.msg);
                msg = new Message(send, id);
                byte[] sendbuffer = msg.message.getBytes();
                sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length, this.group, this.MCAST_RECEIVE_PORT);
            }

            if (sendPacket != null && msg != null) {
                sendInfo(msg, this.sendSocket, sendPacket);
            }


        } catch (Exception e) {
            System.out.println("[EXCEPTION] " + e.getMessage());
            e.printStackTrace();
            return;
        }
    }

    private void sendInfo(Message msg, MulticastSocket sendSocket, DatagramPacket sendPacket) {
        byte[] recieveBuffer = new byte[this.msgSize * 2]; // should be enough
        DatagramPacket receivePacket = new DatagramPacket(recieveBuffer, recieveBuffer.length);
        System.out.println("Sending info");
        try {
            sendSocket.send(sendPacket); //todo this gives a stupid error Address not set in socket
        } catch (IOException e) {
            System.out.println("[EXCEPTION] Couldn't send packet" + e.getMessage());
            e.printStackTrace();
            return;
        }


    }

    private String parseMsg(String msg) {
        String[] msgSplit = msg.split("\\|");
        String type = msgSplit[1].split(":")[1];
        //? System.out.println("msg-" + msg);

        if (type.equals("alive")) {
            String address = msgSplit[3].split(":")[1];
            int port = Integer.parseInt(msgSplit[4].split(":")[1]);
            // update ports
            this.connection.updatePorts(address, port);
            return "type:alive|status:ack|address:" + this.tcpHost + "|port:" + this.sv_port + "|destAddr:" + address + "|destPort:" + port;
        } else if (type.equals("login")) {
            String username = msgSplit[3].split(":")[1];
            String password = msgSplit[4].split(":")[1];
            HashMap<String, User> users = this.db.getUsers(); // TODO, not yet implemented
            if (!users.containsKey(username)) {
                return "type:login|status:fail|msg: User not found";
            }

            User user = users.get(username);
            if (user == null) {
                return "type:login|status:fail|msg: User not found";
            }

            if (users.containsKey(username) && user.password.equals(password)) {
                return "type:login|status:ok|msg: Welcome " + user.username + ", you are logged In!|isAdmin:" + user.admin + "|notify:" + user.notify;
            }

            return "type:login|status:fail|msg: Wrong password";

        }
        return "type:unknown";
    }
}
