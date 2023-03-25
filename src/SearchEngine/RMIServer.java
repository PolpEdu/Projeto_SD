package SearchEngine;

import Client.Client;
import Utility.Message;
import interfaces.ServerInterface;

import java.io.FileInputStream;
import java.io.IOException;

import java.net.*;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.Semaphore;

public class RMIServer extends UnicastRemoteObject implements ServerInterface {
    // number of times to check if server is alive
    static final int alive_checks = 5;

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

    public static void main(String[] args) throws RemoteException {
        System.getProperties().put("java.security.policy", "policy.all");

        RMIServer rmiServer = null;
        Properties prop = new Properties();


        String SETTINGS_PATH = "src\\RMIServer.properties";
        String rmiHost;
        int rmiPort;

        String mcAddress;
        String mcRecievePort;
        int mcSendPort;

        String rmiRegistryName;

        /*
        System.getProperties().put("java.security.policy", "policy.all");
        System.setSecurityManager(new RMISecurityManager());
        */


        try {
            prop.load(new FileInputStream(SETTINGS_PATH));
            rmiHost = prop.getProperty("HOST");
            rmiPort = Integer.parseInt(prop.getProperty("PORT"));

            rmiRegistryName = prop.getProperty("RMI_REGISTRY_NAME");

            mcAddress = prop.getProperty("MC_ADDR");
            mcRecievePort = prop.getProperty("MC_REC_PORT");
            mcSendPort = Integer.parseInt(prop.getProperty("MC_SEND_PORT"));



            // check if any of the properties are null
            if (rmiHost == null || mcAddress == null || mcRecievePort == null || mcSendPort == 0 || rmiPort == 0 || rmiRegistryName == null) {
                System.out.println("[EXCEPTION] Properties file is missing some properties");
                System.out.println("Current config: " + rmiHost + ":" + rmiPort + " " + rmiRegistryName);
                return;
            }

            rmiServer = new RMIServer(mcAddress, mcSendPort, Integer.parseInt(mcRecievePort), null);

        } catch(RemoteException er){
            System.out.println("[EXCEPTION] RemoteException");
            er.printStackTrace();
            return;
        } catch(IOException ei){
            System.out.println("[EXCEPTION] IOException");
            ei.printStackTrace();
            return;
        }

        while (true) {
            try {
                Registry r = LocateRegistry.createRegistry(rmiPort);
                System.setProperty("java.rmi.server.hostname", rmiHost); // set the host name
                r.rebind(rmiRegistryName, rmiServer);
                System.out.println("[SERVER] Running on " + rmiHost+ ":" + rmiPort + "");

                // keep the server running
                rmiServer.loop();

            } catch (RemoteException e) {
                System.out.println("[EXCEPTION] RemoteException, could not create registry. Retrying in 1 second...");
                try {
                    Thread.sleep(1000);
                    rmiServer.hPrincipal = (ServerInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegistryName);

                    rmiServer.backUp(rmiPort, rmiHost, rmiRegistryName);
                } catch (InterruptedException | NotBoundException ei) {
                    System.out.println("[EXCEPTION] InterruptedException | NotBoundException");
                    ei.printStackTrace();
                    return;
                }
            }
        }
    }

    public void backUp(int rmiPort, String rmiHost, String rmiRegistryName) throws NotBoundException, RemoteException, InterruptedException {
        while (true) {
                Thread.sleep(await_time);
                try {
                    // check if server is alive
                    if (this.hPrincipal.alive() == 1) {
                        System.out.println("Server is alive");
                    }
                } catch (RemoteException e) {
                    System.out.println("[SERVER] Getting connection...");

                    for (int i = 0; i < alive_checks; i++) {
                        try {
                            Thread.sleep(await_time);
                            this.hPrincipal = (ServerInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegistryName);
                        } catch (RemoteException er) {
                            System.out.println("[EXCEPTION] RemoteException, could not create registry. Retrying in 1 second...");
                            this.hPrincipal = null;
                        } catch (InterruptedException ei) {
                            System.out.println("[EXCEPTION] InterruptedException");
                            ei.printStackTrace();
                            return;
                        } catch (NotBoundException en) {
                            System.out.println("[EXCEPTION] NotBoundException");
                            en.printStackTrace();
                            return;
                        }
                    }
                }
        }
    }

    public int alive() throws RemoteException {
        return 1;
    }

    @Override
    public void updateClient(String username, Client client) throws RemoteException {
        if (client == null) {
            this.clients.remove(username);
        } else {
            if (this.clients.containsKey(username)) {
                this.clients.replace(username, client);
            } else {
                this.clients.put(username, client);
            }
        }
    }

    @Override
    public ArrayList<String> checkLogin(String username, String password) throws RemoteException {
        ArrayList<String> response = new ArrayList<>();


        return response;
    }

    public static void loop() {
        while (true) {}
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
