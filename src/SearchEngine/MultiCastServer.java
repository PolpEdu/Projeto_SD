package SearchEngine;

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
    public int tcpPort;
    public String tcpHost;
    MulticastSocket receiveSocket;
    MulticastSocket sendSocket;
    InetAddress group;
    LinkedList<String> receivedQueue;
    Downloader downloader;
    //TCPServer tcpServer;
    //private Connection connection;
    HashMap<String, HashSet<Integer>> ports;
    Semaphore conSem;
    public MultiCastServer(String tcpHost, int tcpPort, String multicastAddress, int sendPort, int receivePort){
        this.receiveSocket = null;
        this.sendSocket = null;
        this.group = null;
        this.tcpPort = tcpPort;
        this.tcpHost = tcpHost;
        this.MULTICAST_ADDRESS = multicastAddress;
        this.MULTICAST_SEND_PORT = sendPort;
        this.MULTICAST_RECEIVE_PORT = receivePort;
        this.ports = new HashMap<>();
        this.receivedQueue = new LinkedList<>();
        this.conSem = new Semaphore(1);

    }
    public void run(){
        this.downloader = new Downloader(this.receiveSocket,this.group, this.ports,this.conSem, this.tcpPort, this.tcpHost);
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
