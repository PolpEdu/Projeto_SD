package Utility;

import SearchEngine.Database;
import SearchEngine.Downloader;
import SearchEngine.TCPServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.LinkedList;

// Class used to send and receive messages from Multicast Server
public class Request extends Thread {
    private int msgSize;
    private String msg;
    private LinkedList<String> msgQueue; // Receive Messages Queue

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

    public Request(int msgSize, String msg, LinkedList<String> msgQueue, int MCAST_SEND_PORT, int MCAST_RECEIVE_PORT, InetAddress group, MulticastSocket receiveSocket, MulticastSocket sendSocket, int sv_id, Database db, Downloader downloader, String tcpHost, TCPServer tcpServer) {
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
    }

    public void run() {
        String[] msgSplit = this.msg.split("\\|");
        try {
            // messages will be always composed by 3 parts initially: id | type:<type> | status:<status> | ...
            String id = msgSplit[0].split(":")[1];
            String type = msgSplit[1].split(":")[1];
            String status = msgSplit[2].split(":")[1];


            if (type.equals("alive")) {
                String address = msgSplit[3].split(":")[1];
                int port = Integer.parseInt(msgSplit[4].split(":")[1]);
                if (status.equals("ack")) {
                    String destAddr = msgSplit[5].split(":")[1];
                    int destPort = Integer.parseInt(msgSplit[6].split(":")[1]);
                    if (destAddr.equals(this.tcpHost) && destPort == this.sv_port) {
                        // we are the destination
                        // this.connection.updatePorts(address, port);
                    }
                    return;
                }
                // check if the message is not for this server
                if (!(address.equals(this.tcpHost) && port == this.sv_port)) {
                    String send = parseMsg(this.msg);
                    Message msg = new Message(send, id);
                    byte[] sendbuffer = msg.message.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length, this.group, this.MCAST_RECEIVE_PORT);
                    sendSocket.send(sendPacket);
                }
            }

            sendInfo(id, type, this.sendSocket);
        } catch (Exception e) {
            System.out.println("[EXCEPTION] " + e.getMessage());
            e.printStackTrace();
            return;
        }
    }

    private void sendInfo(String id, String type, MulticastSocket sendSocket) {
        byte[] recieveBuffer = new byte[this.msgSize];
        DatagramPacket receivePacket = new DatagramPacket(recieveBuffer, recieveBuffer.length);
        System.out.println("Sending info kfdsjksadf");
        //todo....
    }

    private String parseMsg(String msg) {
        String[] msgSplit = msg.split("\\|");
        String type = msgSplit[1].split(":")[1];
        switch (type) {
            case "alive":
                String address = msgSplit[3].split(":")[1];
                int port = Integer.parseInt(msgSplit[4].split(":")[1]);
                // update ports
                // this.connection.updatePorts(address, port);
                return "type:alive | status:ack | address:" + this.tcpHost + " | port:" + this.sv_port + " | destAddr:" + address + " | destPort:" + port;
            default:
                return "type:unknown";
        }
    }
}
