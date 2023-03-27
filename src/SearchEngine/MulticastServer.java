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
        System.out.println("[MULTICAST_SERVER] Running...");
        try {
            this.receiveSocket = new MulticastSocket(MULTICAST_RECEIVE_PORT);
            this.sendSocket = new MulticastSocket(MULTICAST_SEND_PORT);
            this.group = InetAddress.getByName(MULTICAST_ADDRESS);
            this.receiveSocket.joinGroup(this.group);
            DatagramPacket receivePacket;

            //initialize downloader
            this.downloader = new Downloader(this.urlQueue, this.receiveSocket, this.MULTICAST_RECEIVE_PORT, this.group, this.ports, this.conSem, this.connection.getTcpPort(), this.tcpHost);


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
