package SearchEngine;

import Client.Client;
import Utility.Message;
import Utility.MulticastSend;
import interfaces.RMIBarrelInterface;
import interfaces.RMIServerInterface;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class RMIServer extends UnicastRemoteObject implements RMIServerInterface {
    // number of times to check if server is alive
    static final int alive_checks = 5;

    // time to wait between checks in milliseconds, if server is alive
    static final int await_time = 1000;


    private final LinkedBlockingQueue<String> urlQueue;

    // HashMap of clients connected to server
    HashMap<String, Client> clients;

    // this is the queue that will store the messages that are waiting for an ack
    LinkedList<Message> sendQueue;

    // Interface for the server that will receive the messages (this class)
    RMIServerInterface hPrincipal;

    // Interface for the barrels
    RMIBarrelInterface b;

    // this is the multicast that will send the messages
    MulticastSend m_Send;

    String bRMIregistry;
    String bRMIhost;
    int bRMIport;

    public RMIServer(LinkedBlockingQueue urlQueue, String multicastAddress, int multicastSendPort, RMIServerInterface hPrincipal, String bRMIregistry, String bRMIhost, int bRMIport) throws RemoteException {
        super();

        this.m_Send = new MulticastSend(multicastAddress, multicastSendPort);

        this.hPrincipal = hPrincipal;

        this.bRMIregistry = bRMIregistry;
        this.bRMIhost = bRMIhost;
        this.bRMIport = bRMIport;

        this.clients = new HashMap<>();
        this.sendQueue = new LinkedList<>();
        this.urlQueue = urlQueue;
    }

    public static void main(String[] args) throws RemoteException {
        System.getProperties().put("java.security.policy", "policy.all");

        Properties prop = new Properties();

        String SETTINGS_PATH = "src\\RMIServer.properties";

        String rmiRegistryName;
        String rmiHost;
        int rmiPort;
        RMIServer rmiServer;

        String bRmiRegistryName;
        String bRmiHost;
        int bRmiPort;


        String mcAddress;
        int mcSendPort;


        try {
            prop.load(new FileInputStream(SETTINGS_PATH));

            rmiHost = prop.getProperty("HOST");
            rmiPort = Integer.parseInt(prop.getProperty("PORT"));

            rmiRegistryName = prop.getProperty("RMI_REGISTRY_NAME");

            mcAddress = prop.getProperty("MC_ADDR");
            mcSendPort = Integer.parseInt(prop.getProperty("MC_SEND_PORT"));

            bRmiHost = prop.getProperty("B_HOST");
            bRmiPort = Integer.parseInt(prop.getProperty("B_PORT"));
            bRmiRegistryName = prop.getProperty("B_RMI_REGISTRY_NAME");


            // check if any of the properties are null
            if (rmiHost == null || mcAddress == null || mcSendPort == 0 || rmiPort == 0 || rmiRegistryName == null || bRmiHost == null || bRmiPort == 0 || bRmiRegistryName == null) {
                System.out.println("[EXCEPTION] Properties file is missing some properties");
                System.out.println("Current config: " + rmiHost + ":" + rmiPort + " " + rmiRegistryName);
                System.out.println("Current config: " + mcAddress + ":" + mcSendPort);
                System.out.println("Current config: " + bRmiHost + ":" + bRmiPort + " " + bRmiRegistryName);
                return;
            }

            UrlQueue urlQueue = new UrlQueue();
            rmiServer = new RMIServer(urlQueue.getUrlQueue(), mcAddress, mcSendPort, null, bRmiRegistryName, bRmiHost, bRmiPort);

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
                // create the registry and bind the server to the current host
                Registry r = LocateRegistry.createRegistry(rmiPort);
                System.setProperty("java.rmi.server.hostname", rmiHost); // set the host name
                r.rebind(rmiRegistryName, rmiServer);
                System.out.println("[SERVER] Running on " + rmiHost + ":" + rmiPort + "->" + rmiRegistryName);

                while (true) {
                    try {
                        rmiServer.b = (RMIBarrelInterface) LocateRegistry.getRegistry(bRmiHost, bRmiPort).lookup(bRmiRegistryName);
                        System.out.println("[SERVER] Got barrel registry on " + bRmiHost + ":" + bRmiPort + "->" + bRmiRegistryName);
                        break;
                    } catch (NotBoundException | RemoteException e1) {
                        Thread.sleep(await_time);
                        System.out.println("[EXCEPTION] NotBoundException | RemoteException, could not get barrel Registry: " + e1.getMessage());
                        System.out.println("Current barrel config: " + bRmiHost + ":" + bRmiPort + " " + bRmiRegistryName);
                    }
                }

                // keep the server running
                loop();
            } catch (RemoteException | InterruptedException e) {
                System.out.println("[EXCEPTION] RemoteException, could not create registry. Retrying in 1 second...");
                try {
                    Thread.sleep(await_time);
                    rmiServer.hPrincipal = (RMIServerInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegistryName);
                    rmiServer.backUpCreate(rmiPort, rmiHost, rmiRegistryName);
                } catch (InterruptedException | NotBoundException ei) {
                    System.out.println("[EXCEPTION] InterruptedException | NotBoundException");
                    ei.printStackTrace();
                    return;
                }
            }
        }
    }

    public static void loop() {
        while (true) {
        }
    }

    public void backUpCreate(int rmiPort, String rmiHost, String rmiRegistryName) throws NotBoundException, RemoteException, InterruptedException {
        while (true) {
            try {
                // check if server is alive
                if (this.hPrincipal.alive()) {
                    System.out.println("[BARREL] Barrel is alive.");
                }
            } catch (RemoteException e) {
                System.out.println("[BARREL] Getting connection...");

                for (int i = 0; i < alive_checks; i++) {
                    try {
                        Thread.sleep(await_time);
                        this.hPrincipal = (RMIServerInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegistryName);
                        break;
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

    public boolean alive() throws RemoteException {
        return true;
    }

    @Override
    public String takeLink() throws RemoteException {

        try {
            return this.urlQueue.take();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void offerLink(String link) throws RemoteException {
        this.urlQueue.offer(link);
    }

    @Override
    public boolean isempty() throws RemoteException {
        return this.urlQueue.isEmpty();
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

    // RETURNS an hashmap with every link found as key and the title as the first value of the object array and the description as the second value
    public HashMap<String, ArrayList<String>> searchLinks(String[] words) throws RemoteException {
        ArrayList<HashSet<String>> totalUrlfound = new ArrayList<HashSet<String>>();
        HashMap<String, ArrayList<String>> res = new HashMap<String, ArrayList<String>>();

        System.out.println("[SERVER] Searching for links. From words: " + Arrays.toString(words) + "");

        for (String w : words) {
            HashSet<String> links = this.b.searchLinks(w);

            if (links == null) {
                System.out.println("[SERVER] Error finding links with word: " + w);
                continue;
            }

            if (links.size() == 0) {
                System.out.println("[SERVER] No Links found for word: " + w);
                continue;
            }
            // check the first element of the hashset, if the first element "failure" is found, then the search failed
            if (links.iterator().next().equals("No barrels available")) {
                System.out.println("[SERVER] Search failed for word: " + w + ". No barrels available.");
                continue;
            }

            totalUrlfound.add(links);
        }

        // we have an array of hashsets, we need to merge them into one Array<Stirng>.
        // only add the links that are in all the hashsets
        ArrayList<String> finalUrlfound = new ArrayList<String>();
        for (HashSet<String> h : totalUrlfound) {
            for (String l : h) {
                if (finalUrlfound.contains(l)) {
                    continue;
                }
                boolean found = true;
                for (HashSet<String> h2 : totalUrlfound) {
                    if (!h2.contains(l)) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    finalUrlfound.add(l);
                }
            }
        }


        System.out.println("[SERVER] Found " + finalUrlfound);


        // add the links to the hashmap as the key and the title as the first value of the object array and the description as the second value
        for (String l : finalUrlfound) {
            ArrayList<String> title = this.b.searchTitle(l);
            ArrayList<String> description = this.b.searchDescription(l);

            if (title == null || description == null) {
                System.out.println("[SERVER] Error finding Title or Description for link: " + l);
                res.put(l, new ArrayList<String>());
                continue;
            } else

            if (title.size() == 0 || description.size() == 0) {
                System.out.println("[SERVER] No Title or Description found for link: " + l);
                //q: is it possible return hashmap with link as key and empty arraylist as value?
                res.put(l, new ArrayList<String>());
                continue;
            } else if (!Objects.equals(title.get(0), "success") || !Objects.equals(description.get(0), "success")) {
                System.out.println("[SERVER] Error finding Title or Description for link: " + l);
                res.put(l, new ArrayList<String>());
                continue;
            }

            // select the first possible title and description for the link from the title and description arraylists
            res.put(l, new ArrayList<String>(Arrays.asList(title.get(1), description.get(1))));
        }

        return res;
    }

    @Override
    public ArrayList<String> searchWord(String word) throws RemoteException {
        return null;
    }

    @Override
    public ArrayList<String> checkLogin(String username, String password) throws RemoteException {
        ArrayList<String> res = this.b.verifyUser(username, password);

        String message = res.get(1);

        if (res.get(0).equals("failure")) {
            // login unsuccessful and not admin
            return new ArrayList<String>(Arrays.asList("false", "false", message));
        }
        String admin = res.get(2);
        // login successful and not admin
        return new ArrayList<String>(Arrays.asList("true", admin, message));
    }

    @Override
    public ArrayList<String> checkRegister(String username, String password, String firstName, String lastName) throws RemoteException {
        ArrayList<String> res = this.b.checkUserRegistration(username, password, firstName, lastName);
        System.out.println("[SERVER] Barrel RMI Response: " + res);

        String message = res.get(1);
        if (res.get(0).equals("failure")) {
            // register unsuccessful and not admin
            return new ArrayList<String>(Arrays.asList("false", "false", message));
        }
        String admin = res.get(2);
        // register successful and not admin
        return new ArrayList<String>(Arrays.asList("true", admin, message));
    }


}