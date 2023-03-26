package SearchEngine;

import Utility.Connection;
import Utility.Message;
import Utility.Request;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;

class MultiCastServer extends Thread {

    private int serverNumber;

    // Object to store the server's data in files
    private Database fileManager;

    // string representation of the multicast address

    public int MULTICAST_SEND_PORT;
    private String MULTICAST_ADDRESS;
    public int MULTICAST_RECEIVE_PORT;
    public String tcpHost;

    public UrlQueue urlQueue;
    MulticastSocket receiveSocket;
    MulticastSocket sendSocket;
    InetAddress group;
    LinkedList<Message> receivedQueue;
    Downloader downloader;
    
    TCPServer tcpServer;
    private Connection connection;
    HashMap<String, HashSet<Integer>> ports;
    Semaphore conSem;
    int messageSize = 1024 * 8;

    public MultiCastServer(String tcpHost, int tcpPort, String multicastAddress, int sendPort, int receivePort) {
        this.MULTICAST_ADDRESS = multicastAddress;
        this.MULTICAST_SEND_PORT = sendPort;
        this.MULTICAST_RECEIVE_PORT = receivePort;

        this.urlQueue = new UrlQueue();

        this.fileManager = new Database(this.serverNumber);

        this.tcpHost = tcpHost;
        this.tcpServer = new TCPServer(tcpPort, this.fileManager);


        this.ports = new HashMap<>();
        this.receivedQueue = new LinkedList<Message>();
        this.receiveSocket = null;
        this.sendSocket = null;
        this.group = null;

        this.conSem = new Semaphore(1);
        this.connection = new Connection(this.tcpHost, tcpPort, this.ports, this.conSem);

    }

    public void run() {
        boolean checked_msg = false;
        System.out.println("[" + this.getName() + "] Running...");
        try {
            this.receiveSocket = new MulticastSocket(MULTICAST_RECEIVE_PORT);
            this.sendSocket = new MulticastSocket(MULTICAST_SEND_PORT);
            this.group = InetAddress.getByName(MULTICAST_ADDRESS);
            this.receiveSocket.joinGroup(this.group);
            DatagramPacket receivePacket;

            //initialize downloader
            this.downloader = new Downloader(this.urlQueue, this.receiveSocket, this.MULTICAST_RECEIVE_PORT, this.group, this.ports, this.conSem, this.connection.getTcpPort(), this.tcpHost);


            try {
                String id = UUID.randomUUID().toString();
                String msgAlive = "id:" + id + "|type:alive|status:online|address:" + this.tcpHost + "|port:" + this.connection.getTcpPort();
                byte[] sendbuffer = msgAlive.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length, this.group, this.MULTICAST_RECEIVE_PORT);
                // print sent message
                System.out.println("[" + this.getName() + "] Sending: " + msgAlive);
                this.receiveSocket.send(sendPacket);
            } catch (IOException e) {
                System.out.println("[EXCEPTION] " + e.getMessage());
                e.printStackTrace();
                return;
            }

            //for now receiving message, we are just recieving, we need to send a message to donwloaders first
            while (true) {
                byte[] receivebuffer = new byte[messageSize];
                receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
                this.receiveSocket.receive(receivePacket);

                String received = new String(receivePacket.getData(), 0, receivePacket.getLength());

                //? System.out.println("[" + this.getName() + "] Received: " + received);

                String[] list = received.split("\\|");
                String id = list[0].split(":")[1];
                String type = list[1].split(":")[1];

                checked_msg = true;

                if (type.equals("alive")) {
                    // System.out.println("[" + this.getName() + "] Starting request thread");
                    Request req = new Request(this.messageSize, received, this.receivedQueue, this.MULTICAST_SEND_PORT, this.MULTICAST_RECEIVE_PORT, this.group, this.receiveSocket, this.sendSocket, this.serverNumber, this.fileManager, this.downloader, this.tcpHost, this.tcpServer, this.connection);
                    req.start();
                } else if (type.equals("links") || type.equals("siteinfo") || type.equals("word")) {
                    //downloader message. we need to send a message to the urlQueue
                    Message message = new Message(UUID.randomUUID().toString(), received);

                    receivedQueue.add(message);
                } else {
                    // check if we have the message in our queue
                    for (Message m : receivedQueue) {
                        if (m.id.equals(id)) {
                            checked_msg = false;
                            if (type.equals("ack")) {
                                this.receivedQueue.remove(m);
                            }
                            break;
                        }
                    }
                    if (checked_msg && type.equals("ack")) {
                        // received an ack for a message we sent
                        byte[] sendbuffer = received.getBytes();
                        try {
                            DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length, this.group, this.MULTICAST_RECEIVE_PORT);
                            this.sendSocket.send(sendPacket);
                        } catch (IOException e) {
                            System.out.println("[EXCEPTION] " + e.getMessage());
                            e.printStackTrace();
                            return;
                        }
                    }
                    if (checked_msg && !type.equals("ack")) {
                        // received a message that is not an ack, but we don't have it in our queue
                        // we need to send an ack to the sender
                        this.receivedQueue.add(new Message(id, received));
                        Request request = new Request(this.messageSize, received, this.receivedQueue, this.MULTICAST_SEND_PORT, this.MULTICAST_RECEIVE_PORT, this.group, this.receiveSocket, this.sendSocket, this.serverNumber, this.fileManager, this.downloader, this.tcpHost, this.tcpServer, this.connection);
                        request.start();
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("[EXCEPTION] " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            InputStream input = new FileInputStream(new File("src/MulticastServer.properties").getAbsoluteFile());
            Properties MulticastServer = new Properties();
            MulticastServer.load(input);
            String tcpHost = MulticastServer.getProperty("HOST");
            int tcpPort = Integer.parseInt(MulticastServer.getProperty("PORT"));
            String multicastAddress = MulticastServer.getProperty("MC_ADDR");
            int sendPort = Integer.parseInt(MulticastServer.getProperty("MC_SEND_PORT"));
            int receivePort = Integer.parseInt(MulticastServer.getProperty("MC_RECEIVE_PORT"));

            MultiCastServer multicastServer = new MultiCastServer(tcpHost, tcpPort, multicastAddress, sendPort, receivePort);
            multicastServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
