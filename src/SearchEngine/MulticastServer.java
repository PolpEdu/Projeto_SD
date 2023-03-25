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

    public UrlQueue urlQueue;
    MulticastSocket receiveSocket;
    MulticastSocket sendSocket;
    InetAddress group;
    LinkedList<Message> receivedQueue;
    Downloader downloader;
    //TCPServer tcpServer;
    //private Connection connection;
    HashMap<String, HashSet<Integer>> ports;
    Semaphore conSem;
    int messageSize = 1024*8;

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
        this.urlQueue = new UrlQueue();

    }
    public void run(){
        byte[] receivebuffer;
        String received;
        System.out.println(this.getName() + " is running...");
        try{
            this.receiveSocket = new MulticastSocket(MULTICAST_RECEIVE_PORT);
            this.sendSocket = new MulticastSocket(MULTICAST_SEND_PORT);
            this.group = InetAddress.getByName(MULTICAST_ADDRESS);
            this.receiveSocket.joinGroup(this.group);
            DatagramPacket receivePacket;

            //initialize downloader
            this.downloader = new Downloader(this.urlQueue, this.receiveSocket,this.group, this.ports,this.conSem, this.tcpPort, this.tcpHost);

            //for now receiving message
            while(true){
                receivebuffer = new byte[messageSize];
                receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
                this.receiveSocket.receive(receivePacket);

                received = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.printf(received);
            }
        }

        catch (IOException e){
            System.out.println("IO: " + e.getMessage());
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
