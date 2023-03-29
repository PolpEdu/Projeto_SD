package SearchEngine;

import Client.User;
import interfaces.RMIBarrelInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class Barrel extends Thread implements RMIBarrelInterface {
    static final int alive_checks = 5;
    static final int await_time = 2000;
    private final int id; // id do barrel

    // multicast from downloaders
    private final String MULTICAST_ADDRESS;
    private final int MULTICAST_RECEIVE_PORT;

    private final HashMap<String, HashSet<String>> word_Links;
    private final HashMap<String, HashSet<String>> link_links;
    private final HashMap<String, ArrayList<String>> link_info;
    private final HashMap<String, HashSet<String>> users;


    private final int rmiPort;
    private final String rmiHost;
    private final String rmiRegister;

    private final File linkfile;
    private final File wordfile;
    private final File infofile;
    private final File usersfile;

    int messageSize = 8 * 1024;
    Database files;

    private InetAddress group;
    private RMIBarrelInterface b;
    private MulticastSocket receiveSocket; // send socket do multicastserver

    public Barrel(int id, int MULTICAST_RECEIVE_PORT, String MULTICAST_ADDRESS, String rmiHost, int rmiPort, String rmiRegister, File linkfile, File wordfile, File infofile, File usersfile, RMIBarrelInterface barrelInterface, Database files) {
        this.id = id;
        this.receiveSocket = null;
        this.group = null;

        this.linkfile = linkfile;
        this.wordfile = wordfile;
        this.infofile = infofile;
        this.usersfile = usersfile;

        this.files = files;

        this.MULTICAST_ADDRESS = MULTICAST_ADDRESS;
        this.MULTICAST_RECEIVE_PORT = MULTICAST_RECEIVE_PORT;

        this.rmiPort = rmiPort;
        this.rmiHost = rmiHost;
        this.rmiRegister = rmiRegister;
        this.b = barrelInterface;

        this.word_Links = new HashMap<>();
        this.link_links = new HashMap<>();
        this.link_info = new HashMap<>();
        this.users = new HashMap<>();
    }

    public static void main(String[] args) {
        System.getProperties().put("java.security.policy", "policy.all");

        try {
            Properties barrelProp = new Properties();
            barrelProp.load(new FileInputStream(new File("src/Barrel.properties").getAbsoluteFile()));

            Properties multicastServerProp = new Properties();
            multicastServerProp.load(new FileInputStream(new File("src/MulticastServer.properties").getAbsoluteFile()));

            // rmi to send register the barrel
            String rmiHost = barrelProp.getProperty("B_HOST");
            String rmiRegister = barrelProp.getProperty("B_RMI_REGISTER");
            int rmiPort = Integer.parseInt(barrelProp.getProperty("B_PORT"));

            // Multicast to receive data from downloaders
            String multicastAddress = multicastServerProp.getProperty("MC_ADDR");
            int receivePort = Integer.parseInt(multicastServerProp.getProperty("MC_RECEIVE_PORT"));

            // create the registry
            Registry r = LocateRegistry.createRegistry(rmiPort);
            System.setProperty("java.rmi.server.hostname", rmiHost); // set the host name

            // parrel interface to rebind the barrel
            RMIBarrelInterface barrelInterface = new Barrel(0, receivePort, multicastAddress, rmiHost, rmiPort, rmiRegister, null, null, null, null, null, null);
            r.rebind(rmiRegister, barrelInterface); // main barrel to receive the register

            for (int i = 1; i < 3; i++) {

                if (rmiHost == null || rmiPort == 0 || rmiRegister == null || multicastAddress == null || receivePort == 0) {
                    System.out.println("[BARREL " + i + "] Error reading properties file");
                    System.exit(1);
                }

                File linkfile = new File("src\\links-" + i);
                File wordfile = new File("src\\words-" + i);
                File infofile = new File("src\\info-" + i);
                File usersfile = new File("src\\users-" + i);

                Database files = new Database(i);
                Barrel barrelt = new Barrel(i, receivePort, multicastAddress, rmiHost, rmiPort, rmiRegister, linkfile, wordfile, infofile, usersfile, barrelInterface, files);
                barrelt.start();
            }

        } catch (RemoteException e) {
            System.out.println("[BARREL] Error creating registry: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("[BARREL] Error reading properties file: " + e.getMessage());
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
            } else {
                System.out.println("[BARREL " + this.id + "] " + received);
            }

            this.files.updateLinks(link_links, linkfile);
            this.files.updateWords(word_Links, wordfile);
            this.files.updateInfo(link_info, infofile);
            // this.files.updateUsers(users, userfile);
        }
    }

    public void run() {
        System.out.println("[BARREL " + this.id + "] Barrel running...");

        try {
            System.out.println("[SERVER] Running on " + rmiHost + ":" + rmiPort + "");

            // Multicast, receive from downloaders
            this.receiveSocket = new MulticastSocket(MULTICAST_RECEIVE_PORT);
            this.group = InetAddress.getByName(MULTICAST_ADDRESS);
            this.receiveSocket.joinGroup(this.group);

            loop();
        } catch (RemoteException e) {
            System.out.println("[BARREL " + this.id + "] RemoteException, could not create registry. Retrying in " + await_time / 1000 + " second...");

            try {
                Thread.sleep(await_time);
                this.b = (RMIBarrelInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegister);
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

    public ArrayList<String> registerUser(String username, String password) throws RemoteException {
        ArrayList<String> response = new ArrayList<>();

        return response;
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
                        this.b = (RMIBarrelInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegister);
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

    @Override
    public boolean alive() throws RemoteException {
        return false;
    }

    @Override
    public ArrayList<String> checkUserRegistration(String username, String password, String firstName, String lastName) throws RemoteException {
        HashMap<String, User> users = this.files.getUsers();
        if (users.containsKey(username)) {
            // "type:register | status:failure | message:User already exists"
            return new ArrayList<>(Arrays.asList("register", "failure", "User already exists"));
        }

        // if no users, make first user admin
        if (users.size() == 0) {
            // "type:register | status:success | message:User registered"
            users.put(username, new User(username, password, true, firstName, lastName));
            this.files.updateUsers(users);
            return new ArrayList<>(Arrays.asList("register", "success", "Admin User registered", "true"));
        }

        // "type:register | status:success | message:User registered"
        users.put(username, new User(username, password, false, firstName, lastName));
        this.files.updateUsers(users);
        return new ArrayList<>(Arrays.asList("register", "success", "User registered", "false"));
    }
}
