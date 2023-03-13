package SearchEngine;

import Client.Client;
import Utility.Message;
import interfaces.ServerInterface;

import java.io.FileInputStream;
import java.io.IOException;

import java.net.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.Semaphore;

public class RMIServer extends UnicastRemoteObject implements ServerInterface {
    // number of times to check if server is alive
    static final int checks = 5;

    // time to wait between checks in milliseconds, if server is alive
    static final int await_time = 1000;

    // HashMap of clients connected to server
    HashMap<String, Client> clients;

    // this is the queue that will store the messages that are waiting for an ack
    LinkedList<Message> sendQueue;

    // Interface for the server that will receive the messages (this class)
    ServerInterface hPrincipal;

    // this is the multicast that will receive the messages
    MulticastReceive m_Receive;

    // this is the multicast that will send the messages
    MulticastSend m_Send;

    RMIServer(String multicastAddress, int multicastSendPort, int multicastReceivePort, ServerInterface hPrincipal) throws RemoteException {
        super();

        this.m_Receive = new MulticastReceive(this.m_Send, multicastAddress, multicastReceivePort);
        this.m_Receive.start();

        this.m_Send = new MulticastSend(multicastAddress, multicastSendPort);

        this.hPrincipal = hPrincipal;

        this.clients = new HashMap<>();
        this.sendQueue = new LinkedList<>();
    }

    public static void main(String[] args) {

        /*
        System.getProperties().put("java.security.policy", "policy.all");
        System.setSecurityManager(new RMISecurityManager());
        */

        RMIServer server = null;
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("MulticastServer.properties"));

        } catch(RemoteException er){
            System.out.println("[EXCEPTION] RemoteException");
            return;
        } catch(IOException ei){}




    }

    public int alive() throws RemoteException {
        return 1;
    }
}


class MulticastReceive extends Thread {
    // this is the multicast that will receive the messages
    private String MULTICAST_ADDRESS;

    // this is the port that will receive the messages
    private int PORT ;

    // size of the message in bytes
    int messageSize = 1024*8;

    // this is the group that will receive the messages
    InetAddress group;

    MulticastSend multicastSend;
    MulticastSocket socket;

    Semaphore sem;


    public MulticastReceive(MulticastSend multicastSend,String multicastAddress, int receivePort) {
        this.sem = new Semaphore(1);
        this.multicastSend = multicastSend;
        this.MULTICAST_ADDRESS = multicastAddress;
        this.PORT = receivePort;

        try{
            this.socket = new MulticastSocket(this.PORT);
            this.group = InetAddress.getByName(this.MULTICAST_ADDRESS);
        } catch(UnknownHostException eu){
        } catch(IOException ei){}
    }

}

class MulticastSend {
    private String MULTICAST_ADDRESS;
    private int PORT ;
    MulticastSocket socket;
    InetAddress group;

    public MulticastSend(String multicastAddress, int sendPort) {
        try{
            this.PORT = sendPort;
            this.MULTICAST_ADDRESS = multicastAddress;
            this.socket = new MulticastSocket(this.PORT);
            this.group = InetAddress.getByName(this.MULTICAST_ADDRESS);
        } catch(UnknownHostException eu){
        } catch(IOException ei){}
    }

}
