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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

// needs to be serializable to be sent through rmi
public class IndexBarrel extends UnicastRemoteObject implements RMIBarrelInterface {
    static final int alive_checks = 5;
    static final int await_time = 2000;

    private ArrayList<Barrel> barrels_threads;

    RMIBarrelInterface b;

    public IndexBarrel() throws RemoteException {
        super();
        // create a list of barrel threads
        this.barrels_threads = new ArrayList<>();
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


            IndexBarrel mainBarrel = new IndexBarrel();
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

            for (int i = 1; i < 2; i++) {

                if (rmiHost == null || rmiPort == 0 || rmiRegister == null || multicastAddress == null || receivePort == 0) {
                    System.out.println("[BARREL " + i + "] Error reading properties file");
                    System.exit(1);
                }

                File linkfile = new File("src\\links-" + i);
                File wordfile = new File("src\\words-" + i);
                File infofile = new File("src\\info-" + i);
                File usersfile = new File("src\\users-" + i);

                Database files = new Database(i);
                Barrel barrel_t = new Barrel(i, receivePort, multicastAddress, rmiHost, rmiPort, rmiRegister, linkfile, wordfile, infofile, usersfile, mainBarrel.b, files, sendPort);
                mainBarrel.barrels_threads.add(barrel_t);
                barrel_t.start();
            }
        } catch (RemoteException e) {
            System.out.println("[BARREL] Error creating registry: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("[BARREL] Error reading properties file: " + e.getMessage());
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

    private Barrel selectBarrelToExcute() {
        // select a random barrel to fulfill the task
        if (this.barrels_threads.size() == 0) {
            System.out.println("[BARREL-INTERFACE] No barrels to fulfill the task");
            // no barrels to fulfill the task
            return null;
        }

        int random = (int) (Math.random() * this.barrels_threads.size());
        return this.barrels_threads.get(random);
    }
}
