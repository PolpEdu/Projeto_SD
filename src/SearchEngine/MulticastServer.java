package SearchEngine;

import Utility.Message;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

class MultiCastServer extends Thread {

    private int serverNumber;

    // Object to store the server's data in files
    private Database fileManager;

    // string representation of the multicast address
    private String MULTICAST_ADDRESS;
    public int MULTICAST_SEND_PORT;
    public int MULTICAST_RECEIVE_PORT;
    public int tcpPort;
    public String tcpHost;
    MulticastSocket receiveSocket;
    MulticastSocket sendSocket;
    InetAddress group;
    LinkedList<Message> receivedQueue;
    Downloader downloader;
    //TCPServer tcpServer;
    private Connection connection;
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
            InputStream input = new FileInputStream(new File("MulticastServer.properties"));
            Properties MulticastServer = new Properties();
            MulticastServer.load(input);
            String tcpHost = MulticastServer.getProperty("tcpHost");
            int tcpPort = Integer.parseInt(MulticastServer.getProperty("tcpPort"));
            String multicastAddress = MulticastServer.getProperty("multicastAddress");
            int sendPort = Integer.parseInt(MulticastServer.getProperty("multicastSendPort"));
            int receivePort = Integer.parseInt(MulticastServer.getProperty("multicastReceivePort"));


            MulticastServer myServer = new MulticastServer(tcpHost,tcpPort,multicastAddress, sendPort,receivePort);
            myServer.start();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }







}
