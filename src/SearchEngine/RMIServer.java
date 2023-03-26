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
import java.util.*;
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

        } catch (RemoteException er) {
            System.out.println("[EXCEPTION] RemoteException");
            er.printStackTrace();
            return;
        } catch (IOException ei) {
            System.out.println("[EXCEPTION] IOException");
            ei.printStackTrace();
            return;
        }

        while (true) {
            try {
                Registry r = LocateRegistry.createRegistry(rmiPort);
                System.setProperty("java.rmi.server.hostname", rmiHost); // set the host name
                r.rebind(rmiRegistryName, rmiServer);
                System.out.println("[SERVER] Running on " + rmiHost + ":" + rmiPort + "");

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

    @Override
    public ArrayList<String> checkRegister(String username, String password, String firstName, String lastName) throws RemoteException {
        String id = UUID.randomUUID().toString();

        Message msg = new Message("type:register|username:" + username + "|password:" + password + "|firstName:" + firstName + "|lastName:" + lastName, id);
        this.m_Send.sendInfo(msg, this.sendQueue);
        String[] response = this.m_Receive.parseRecievedPacket(msg, this.sendQueue).split("\\|");
        this.sendQueue.remove(msg);

        String status = response[2].split(":")[1];
        String message = response[3].split(":")[1];

        Message msgRes = new Message("type:ack", id);
        this.m_Send.sendInfo(msgRes, this.sendQueue);

        if(status.equals("error")) {
            // register unsuccessful and not admin
            return new ArrayList<String>(Arrays.asList("false", "false", message));
        }

        String admin = response[4].split(":")[1];
        if(admin.equals("true")) {
            // register successful and admin
            return new ArrayList<String>(Arrays.asList("true", "true", message));
        }

        // register successful and not admin
        return new ArrayList<String>(Arrays.asList("true", "false", message));
    }

    public static void loop() {
        while (true) {
        }
    }
}


class MulticastReceive extends Thread {
    // this is the multicast that will receive the messages
    private String MULTICAST_ADDRESS;

    // this is the port that will receive the messages
    private int PORT;

    // size of the message in bytes
    int messageSize = 1024 * 8;

    // socket Timeout
    int socketTimeout = 1500;

    // this is the group that will receive the messages
    InetAddress group;

    MulticastSend multicastSend;
    MulticastSocket socket;

    Semaphore sem;


    public MulticastReceive(MulticastSend multicastSend, String multicastAddress, int receivePort) {
        this.sem = new Semaphore(1);
        this.multicastSend = multicastSend;
        this.MULTICAST_ADDRESS = multicastAddress;
        this.PORT = receivePort;

        try {
            this.socket = new MulticastSocket(this.PORT);
            this.group = InetAddress.getByName(this.MULTICAST_ADDRESS);
        } catch (UnknownHostException eu) {
            System.out.println("[EXCEPTION] UnknownHostException");
            eu.printStackTrace();
            return;
        } catch (IOException ei) {
            System.out.println("[EXCEPTION] IOException");
            ei.printStackTrace();
            return;
        }
    }

    public void run() {
        try {
            // wait for the socket to be ready
            this.socket.setSoTimeout(socketTimeout);
            this.socket.joinGroup(this.group);
            while (true) {
            }
        } catch (IOException e) {
            System.out.println("[EXCEPTION] SocketException | IOException");
            e.printStackTrace();
            socket.close();
            return;
        }
    }

    public String parseRecievedPacket(Message msg, LinkedList<Message> sendQueue) {
        String message = null;
        boolean checkInQueue;
        while (message == null) {
            byte[] buffer = new byte[messageSize];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                this.socket.receive(packet);
                message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("[RECEIVED] " + message);
                String[] messageArray = message.split("\\|");
                String id = messageArray[1].split(":")[1];
                if(msg.id.equals(id)) {
                    System.out.println("[RECEIVED] Received our first ACK: "+ message);
                } else {
                    checkInQueue = false;
                    for (Message m : sendQueue) {
                        if (m.id.equals(id)) {
                            checkInQueue = true; //
                            break;
                        }
                    }
                    if (!checkInQueue) {
                        System.out.println("[RECEIVED] Received another ACK: "+ message);
                        Message msgACK = new Message("type:ack", id);
                        this.multicastSend.sendInfo(msgACK, sendQueue); // send again
                    } else {
                        buffer = new byte[messageSize];
                        packet = new DatagramPacket(buffer, buffer.length, this.group, this.PORT);
                        this.socket.send(packet);
                    }
                    message = null;
                }
            } catch (SocketTimeoutException e) {
                System.out.println("[EXCEPTION] SocketTimeoutException: "+ e.getMessage());
                this.multicastSend.sendInfo(msg, sendQueue); // send again
                return null;
            } catch (IOException e) {
                System.out.println("[EXCEPTION] IOException: "+ e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
        return message;
    }

}

class MulticastSend {
    private String MULTICAST_ADDRESS;
    private int PORT;

    MulticastSocket socket;
    InetAddress group;

    public MulticastSend(String multicastAddress, int sendPort) {
        try {
            this.PORT = sendPort;
            this.MULTICAST_ADDRESS = multicastAddress;
            this.socket = new MulticastSocket(this.PORT);
            this.group = InetAddress.getByName(this.MULTICAST_ADDRESS);
        } catch (UnknownHostException eu) {
            System.out.println("[EXCEPTION] UnknownHostException");
            eu.printStackTrace();
            return;
        } catch (IOException ei) {
            System.out.println("[EXCEPTION] IOException");
            ei.printStackTrace();
            return;
        }
    }

    public void sendInfo(Message m, LinkedList<Message> sendQueue) {
        // check if message type is ack
        String type = m.getType();
        byte[] buffer = m.message.getBytes();

        if (!type.equals("ack")) {
            // add message to send queue
            sendQueue.offer(m);
        }

        try {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, this.group, this.PORT);
            this.socket.send(packet);
        } catch (IOException e) {
            System.out.println("[EXCEPTION] IOException");
            e.printStackTrace();
            return;
        }
    }
}
