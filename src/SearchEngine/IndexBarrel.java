package SearchEngine;

import Client.User;
import interfaces.RMIBarrelInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.Semaphore;

// needs to be serializable to be sent through rmi
public class IndexBarrel extends UnicastRemoteObject implements RMIBarrelInterface {
    static final int alive_checks = 5;
    static final int await_time = 2000;

    RMIBarrelInterface b;
    private ArrayList<Barrel> barrels_threads;

    private int id;

    public IndexBarrel(int id) throws RemoteException {
        super();
        // create a list of barrel threads
        this.barrels_threads = new ArrayList<>();
        this.id = id;
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
            int sendPort = Integer.parseInt(multicastServerProp.getProperty("MC_SEND_PORT"));

            int id = 1;
            IndexBarrel mainBarrel = new IndexBarrel(id);
            try {
                // create the registry
                Registry r = LocateRegistry.createRegistry(rmiPort);
                System.setProperty("java.rmi.server.hostname", rmiHost); // set the host name

                // parrel interface to rebind the barrel
                r.rebind(rmiRegister, mainBarrel); // main barrel to receive the register
                System.out.println("[BARREL-INTERFACE] BARREL RMI registry created on: " + rmiHost + ":" + rmiPort + "->" + rmiRegister);

            } catch (RemoteException e) {
                System.out.println("[BARREL-INTERFACE] RemoteException, could not create registry. Retrying in " + await_time / 1000 + " second...");

                try {
                    Thread.sleep(await_time);
                    mainBarrel.b = (RMIBarrelInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegister);
                    mainBarrel.backUp(rmiPort, rmiHost, rmiRegister);
                } catch (InterruptedException | NotBoundException | RemoteException ei) {
                    System.out.println("[EXCEPTION] InterruptedException | NotBoundException | RemoteException");
                    ei.printStackTrace();
                }
            }

            Semaphore ackSem = new Semaphore(1);
            for (int i = 1; i < 2; i++) {

                if (rmiHost == null || rmiPort == 0 || rmiRegister == null || multicastAddress == null || receivePort == 0) {
                    System.out.println("[BARREL " + i + "] Error reading properties file");
                    System.exit(1);
                }

                File linkfile = new File("src\\links-" + i);
                File wordfile = new File("src\\words-" + i);
                File infofile = new File("src\\info-" + i);

                Database files = new Database(i);
                Barrel barrel_t = new Barrel(i ,receivePort, multicastAddress,  linkfile, wordfile, infofile,files, ackSem);
                mainBarrel.barrels_threads.add(barrel_t);
                barrel_t.start();
            }
        } catch (RemoteException e) {
            System.out.println("[BARREL] Error creating registry: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("[BARREL] Error reading properties file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void backUp(int rmiPort, String rmiHost, String rmiRegister) throws RemoteException {
        while (true) {
            try {
                Barrel barrel_t = selectBarrelToExcute();

                if (this.b.alive()) {
                    System.out.println("[BARREL] Connection to RMI server reestablished");
                    break;
                }
            } catch (RemoteException e) {
                System.out.println("[BARREL] RemoteException, Getting connection, retrying in " + await_time / 1000 + " second(s)...");
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
        Barrel barrel = this.selectBarrelToExcute();
        return barrel != null;
    }

    @Override
    public ArrayList<String> checkUserRegistration(String username, String password, String firstName, String lastName) throws RemoteException {
        // get the barrel to register the user
        Barrel barrel = this.selectBarrelToExcute();
        if (barrel == null) {
            // "status:failure | message:No barrels available"
            return new ArrayList<>(Arrays.asList("failure", "No barrels available"));
        }

        HashMap<String, User> users = barrel.files.getUsers();
        if (users.containsKey(username)) {
            // "status:failure | message:User already exists"
            return new ArrayList<>(Arrays.asList("failure", "User already exists"));
        }

        // if no users, make first user admin
        if (users.size() == 0) {
            // "status:success | message:User registered"
            users.put(username, new User(username, password, true, firstName, lastName));
            barrel.files.updateUsers(users);
            return new ArrayList<>(Arrays.asList("success", "Admin User registered", "true"));
        }

        // "status:success | message:User registered"
        users.put(username, new User(username, password, false, firstName, lastName));
        barrel.files.updateUsers(users);
        return new ArrayList<>(Arrays.asList("success", "User registered", "false"));
    }

    public ArrayList<String> verifyUser(String username, String password) throws RemoteException {
        // get the barrel to register the user
        Barrel barrel = this.selectBarrelToExcute();
        if (barrel == null) {
            // "status:failure | message:No barrels available"
            return new ArrayList<>(Arrays.asList("failure", "No barrels available"));
        }

        HashMap<String, User> users = barrel.files.getUsers();
        if (!users.containsKey(username)) {
            // "status:failure | message:User does not exist"
            return new ArrayList<>(Arrays.asList("failure", "User does not exist"));
        }

        User user = users.get(username);
        if (!user.password.equals(password)) {
            // "status:failure | message:Wrong password"
            return new ArrayList<>(Arrays.asList("failure", "Wrong password"));
        }

        // "status:success | message:User verified"
        return new ArrayList<>(Arrays.asList("success", "User verified", Boolean.toString(user.admin)));
    }

    @Override
    public HashSet<String> searchLinks(String word) throws RemoteException {
        Barrel barrel = this.selectBarrelToExcute();
        if (barrel == null) {
            // "status:failure | message:No barrels available"
            // return an Hashset with status and message
            return new HashSet<>(Arrays.asList("failure","No barrels available"));
        }

        return barrel.getLinksAssciatedWord(word);
    }

    @Override
    public ArrayList<String> searchTitle(String word) throws RemoteException {
        if (word == null) {
            // "status:failure | message:Word is null"
            return new ArrayList<>(Arrays.asList("failure", "Word is null"));
        }

        Barrel barrel = this.selectBarrelToExcute();
        if (barrel == null) {
            // "status:failure | message:No barrels available"
            return new ArrayList<>(Arrays.asList("failure", "No barrels available"));
        }

        // System.out.println("[BARREL-INTERFACE] Searching for title: " + word);
        String title = barrel.getLinkTitle(word);

        // System.out.println("[BARREL-INTERFACE] Links title: " + title);

        if (title == null) {
            // "status:failure | message:Link does not exist"
            return new ArrayList<>(Arrays.asList("failure", "Link does not exist"));
        }

        // return an array list with the first element as the status
        // and the rest with links
        ArrayList<String> result = new ArrayList<>();
        result.add("success");
        result.add(title);
        return result;
    }

    @Override
    public ArrayList<String> searchDescription(String word) throws RemoteException {
        if (word == null) {
            // "status:failure | message:Word is null"
            return new ArrayList<>(Arrays.asList("failure", "Word is null"));
        }

        Barrel barrel = this.selectBarrelToExcute();
        if (barrel == null) {
            // "status:failure | message:No barrels available"
            return new ArrayList<>(Arrays.asList("failure", "No barrels available"));
        }

        String linksInfo = barrel.getLinkDescription(word);
        // System.out.println("[BARREL-INTERFACE] Links description: " + linksInfo);
        if (linksInfo == null) {
            // "status:failure | message:Link does not exist"
            return new ArrayList<> (Arrays.asList("failure", "Link does not exist"));
        }
        // return links linksInfo with ArrayList
        ArrayList<String> result = new ArrayList<>();
        result.add("success");
        result.add(linksInfo);
        return result;
    }

    @Override
    public HashSet<String> linkpointers(String link) throws RemoteException {
        Barrel barrel = this.selectBarrelToExcute();
        if (barrel == null) {
            // "status:failure | message:No barrels available"
            return new HashSet<>(Arrays.asList("failure", "No barrels available"));
        }

        // System.out.println("[BARREL-INTERFACE] Searching for linkpointers: " + link);

        return barrel.getLinkPointers(link);
    }

    @Override
    public boolean isAdmin(String username) throws RemoteException {
        Barrel barrel = this.selectBarrelToExcute();
        if (barrel == null) {
            // "status:failure | message:No barrels available"
            return false;
        }

        HashMap<String, User> users = barrel.files.getUsers();
        if (!users.containsKey(username)) {
            // "status:failure | message:User does not exist"
            return false;
        }

        User user = users.get(username);
        return user.admin;
    }

    private Barrel selectBarrelToExcute() {
        // select a random barrel to fulfill the task
        if (this.barrels_threads.size() == 0) {
            System.out.println("[BARREL-INTERFACE] No barrels to fulfill the task");
            // no barrels to fulfill the task
            return null;
        }

        int random = (int) (Math.random() * this.barrels_threads.size());

        // check if barrel is alive if not remove from barrels_threads and select another barrel
        if (!this.barrels_threads.get(random).isAlive()) {
            System.out.println("[BARREL-INTERFACE] Barrel " + random + " is not alive");
            this.barrels_threads.remove(random);
            return this.selectBarrelToExcute();
        }

        return this.barrels_threads.get(random);
    }
}
