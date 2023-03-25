package SearchEngine;

import Utility.Message;

import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

class MultiCastServer extends Thread {
    // current number of the server, serves as an identifier
    private int serverNumber;

    // Object to store the server's data in files
    private Database fileManager;

    // string representation of the multicast address
    private String MULTICAST_ADDRESS;

    // port to send multicast packets to
    public int MULTICAST_SEND_PORT;

    // port to receive multicast packets from
    public int MULTICAST_RECEIVE_PORT;

    // port to receive TCP packets from
    public int PORT;

    // Address to receive TCP packets from
    public String TCP_HOST;


    // socket to receive multicast packets
    MulticastSocket receiveSocket;

    // socket to send multicast packets
    MulticastSocket sendSocket;

    // multicast group
    InetAddress group;


    // queue of messages to be sent
    LinkedList<Message> receivedQueue;

    // queue of messages to be sent
    Downloader downloader;

    // Online Ports of the servers in the network
    HashMap<String, HashSet<Integer>> ports;

    public static void main(String[] args) {
        Downloader dl = new Downloader();

        dl.QueueInfo();
    }







}
