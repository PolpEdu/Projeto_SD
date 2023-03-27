package SearchEngine;

import interfaces.RMIServerInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

public class Barrel extends Thread {
    static final int alive_checks = 5;
    static final int await_time = 2000;
    private final String MULTICAST_ADDRESS;
    private final int MULTICAST_RECEIVE_PORT;
    private final int id;
    private final HashMap<String, HashSet<String>> word_Links;
    private final HashMap<String, HashSet<String>> link_links;
    private final HashMap<String, ArrayList<String>> link_info;
    private final int rmiPort;
    private final String rmiHost;
    private final String rmiRegister;
    int messageSize = 8 * 1024;
    private RMIServerInterface b;
    private InetAddress group;
    private MulticastSocket receiveSocket;// send socket do multicastserver

    private File barrelfile;

    public Barrel(int id, int MULTICAST_RECEIVE_PORT, String MULTICAST_ADDRESS, String rmiHost, int rmiPort, String rmiRegister, RMIServerInterface b) {
        this.id = id;
        this.receiveSocket = null;
        this.group = null;

        this.MULTICAST_ADDRESS = MULTICAST_ADDRESS;
        this.MULTICAST_RECEIVE_PORT = MULTICAST_RECEIVE_PORT;

        this.rmiPort = rmiPort;
        this.rmiHost = rmiHost;
        this.rmiRegister = rmiRegister;
        this.b = b;

        this.word_Links = new HashMap<>();
        this.link_links = new HashMap<>();
        this.link_info = new HashMap<>();
    }

    public static void main(String[] args) {
        System.getProperties().put("java.security.policy", "policy.all");

        try {
            Properties barrelProp = new Properties();
            barrelProp.load(new FileInputStream(new File("src/Barrel.properties").getAbsoluteFile()));

            Properties multicastServerProp = new Properties();
            multicastServerProp.load(new FileInputStream(new File("src/MulticastServer.properties").getAbsoluteFile()));


            String rmiHost = barrelProp.getProperty("HOST");
            String rmiRegister = barrelProp.getProperty("RMI_REGISTER");
            int rmiPort = Integer.parseInt(barrelProp.getProperty("PORT"));


            String multicastAddress = multicastServerProp.getProperty("MC_ADDR");
            int receivePort = Integer.parseInt(multicastServerProp.getProperty("MC_RECEIVE_PORT"));

            RMIServerInterface b = (RMIServerInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegister);

            for (int i = 0; i < 2; i++) {

                if (rmiHost == null || rmiPort == 0 || multicastAddress == null || receivePort == 0) {
                    System.out.println("[BARREL " + i + "] Error reading properties file");
                    System.exit(1);
                }

                Barrel barrel = new Barrel(i, receivePort, multicastAddress, rmiHost, rmiPort, rmiRegister, b);
                barrel.start();
            }

        } catch (IOException e) {
            System.out.println("[BARREL] Error reading properties file: " + e.getMessage());
            e.printStackTrace();
        } catch (NotBoundException e) {
            System.out.println("[BARREL] Error connecting to RMI server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loop() throws IOException {
        while (true) {
            byte[] receivebuffer = new byte[messageSize];
            DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
            this.receiveSocket.receive(receivePacket);

            String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
            String[] list = received.split("\\|");
            String id = list[0].split(":")[1];
            String type = list[1].split(":")[1];

            if (id.equals("dwnl")) {
                if (type.equals("word")) {
                    if (!this.word_Links.containsKey(list[2])) {
                        this.word_Links.put(list[2], new HashSet<>());
                    }
                    this.word_Links.get(list[2]).add(list[3]);
                    //System.out.println("test " + list[2] +" " +list[3]);
                } else if (type.equals("links")) {
                    if (!this.link_links.containsKey(list[2])) {
                        this.link_links.put(list[2], new HashSet<>());
                    }
                    this.link_links.get(list[2]).add(list[3]);
                    //System.out.println("test " + list[2] + " " + list[3]);
                } else if (type.equals("siteinfo")) {
                    if (!this.link_info.containsKey(list[2])) {
                        this.link_info.put(list[2], new ArrayList<>());
                    }

                    this.link_info.get(list[2]).add(list[3]);
                    this.link_info.get(list[2]).add(list[4]);
                }
            }
            System.out.println("[BARREL " + this.id + "] " + received);
        }
    }

    public void run() {
        System.out.println("[BARREL " + this.id + "] Barrel running...");

        try {
            this.receiveSocket = new MulticastSocket(MULTICAST_RECEIVE_PORT);
            this.group = InetAddress.getByName(MULTICAST_ADDRESS);
            this.receiveSocket.joinGroup(this.group);

            loop();
            //System.out.println("[BARREL " + (this.id+1) + "]" + received);
        } catch (RemoteException e) {
            System.out.println("[BARREL " + this.id + "] RemoteException, could not create registry. Retrying in " + await_time / 1000 + " second...");

            try {
                Thread.sleep(await_time);
                this.b = (RMIServerInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegister);
                this.backUp(rmiPort, rmiHost, rmiRegister);
            } catch (InterruptedException | NotBoundException | RemoteException ei) {
                System.out.println("[EXCEPTION] InterruptedException | NotBoundException | RemoteException");
                ei.printStackTrace();
            }

        } catch (UnknownHostException e) {
            System.out.println("[EXCEPTION] UnknownHostException");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void backUp(int rmiPort, String rmiHost, String rmiRegister) throws RemoteException {
        while (true) {
            try {
                if (this.b.alive()) {
                    System.out.println("[BARREL " + this.id + "] Connection to RMI server reestablished");
                    break;
                }
            } catch (RemoteException e) {
                System.out.println("[BARREL " + this.id + "] RemoteException, Getting connection, retrying in " + await_time / 1000 + " second(s)...");
                for (int i = 0; i < alive_checks; i++) {
                    try {
                        Thread.sleep(await_time);
                        this.b = (RMIServerInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegister);
                    } catch (RemoteException er) {
                        System.out.println("[EXCEPTION] RemoteException, could not create registry. Retrying in " + +await_time / 1000 + " second(s)...");
                        this.b = null;
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
}
