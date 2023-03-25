package SearchEngine;

import interfaces.ClientInterface;
import interfaces.ServerInterface;

import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import Client.Client;

class RMIClient extends UnicastRemoteObject implements ClientInterface {
    static final int keepAliveTime = 5000;

    private ServerInterface sv;
    private Client client;

    private final String rmiHost;
    private final int rmiPort;
    private final String rmiRegistryName;

    public RMIClient(ServerInterface svInterface, Client client, String rmiHost, int rmiPort, String rmiRegistryName) throws RemoteException{
        super();
        this.sv = svInterface;
        this.client = client;
        this.rmiHost = rmiHost;
        this.rmiPort = rmiPort;
        this.rmiRegistryName = rmiRegistryName;
    }

    public static void main(String[] args) {
        String rmiHost;
        int rmiPort;

        String rmiRegistryName;

        String SETTINGS_PATH = "src\\RMIClient.properties";

        try {
            InputStream config = new FileInputStream(SETTINGS_PATH);
            Properties prop = new Properties();
            prop.load(config);

            rmiHost = prop.getProperty("HOST");
            rmiPort = Integer.parseInt(prop.getProperty("PORT"));
            rmiRegistryName = prop.getProperty("RMI_REGISTRY_NAME");

            if (rmiHost == null || rmiPort == 0 || rmiRegistryName == null) {
                System.out.println("[EXCEPTION] Properties file is missing some properties");
                System.out.println("[EXCEPTION] Current config: " + rmiHost + ":" + rmiPort + " " + rmiRegistryName);
                return;
            }

            System.out.println("[CLIENT] Running on " + rmiHost + ":" + rmiPort);


            // GET SERVER INTERFACE USING REGISTRY
            ServerInterface svInterface = (ServerInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegistryName);


            Client client = new Client("Anon", false);
            RMIClient rmi_client = new RMIClient(svInterface, client, rmiHost, rmiPort, rmiRegistryName);
            rmi_client.menu();

        } catch (RemoteException e){
            System.out.println("[CLIENT] RemoteException");
            e.printStackTrace();
            return;
        } catch (IOException e) {
            System.out.println("[CLIENT] IOException");
            e.printStackTrace();
            return;
        } catch (NotBoundException e) {
            System.out.println("[CLIENT] NotBoundException");
            e.printStackTrace();
            return;
        }
    }

    private void printMenus(int type) {

        switch (type) {
            case 0:
                // Login or Register
                System.out.print("\n### Login ###\n1.Search words\n2.Search Link\n  3.Login\n  4.Register\n   e.Exit\n --> Choice: ");
                return;
            case 1:
                // admin - main menu
                System.out.print("\n### Admin User Panel ###\n1.Search words\n2.Search Link\n3.Index new URL\n4.User List\n5.Give admin Perms\n6.History\n7.Logout\n  e.Exit\n --> Choice: ");
                return;
            case 2:
                // user - main menu
                System.out.print("\n### User Panel ###\n1.Search words\n2.Search Link\n3.History\n  4.Logout\n   e.Exit\n --> Choice: ");
                return;
            case 3:
                System.out.print("\n### Admin Panel ###\n1.Top 10 pages\n2.Top 10 searches\n3.Multicast Servers\nb.Back\n  e.Exit\n --> Choice: ");
                return;
            default:
                System.out.println("[EXCEPTION] Invalid menu type");
                // exit program
                System.exit(1);
                return;
        }
    }

    private void menu() {
        InputStream in  = System.in;
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        // create a new client object
        this.client = new Client("Anon", false);

        while (true) {
            // Anonymous user, not logged yet
            if (this.client.username.equals("Anon")) {
                // Login or Register
                printMenus(0);
                registerLogic(br);

            } else {
                // Admin - main menu
                printMenus(1);
            }
        }

    }

    private void registerLogic(BufferedReader br) {
        String choice = "";

        try{
            choice = br.readLine();
        } catch(IOException ei){
            System.out.println("EXCEPTION: IOException");
            return;
        }

        switch (choice.toLowerCase()) {
            case "1":
                // Search words
                // searchWords(br);
                break;
            case "2":
                // Search Link
                // searchLink(br);
                break;
            case "3":
                // Login
                login(br);
                break;
            case "4":
                // Register
                // register(br);
                break;
            case "e":
                // Exit
                System.out.println("[CLIENT] Exiting...");
                System.exit(0);
                break;
            default:
                System.out.println("[CLIENT] Invalid choice");
                break;
        }
    }

    private void login(BufferedReader br) {
        String username = "";
        String password = "";

        while (username.equals("") || password.equals("")) {
            try {
                System.out.print("\n###LOGIN###\n  Username: ");
                username = br.readLine();

                while (username.length() < 4 || username.length() > 20) {
                    System.out.println("[CLIENT] Username must be between 4 and 20 characters\n\n  Username: ");
                    username = br.readLine();
                }

                System.out.print("  Password: ");
                password = br.readLine();

                while(password.length() < 4 || password.length() > 20) {
                    System.out.println("[CLIENT] Password must be between 4 and 20 characters\n\n  Password: ");
                    password = br.readLine();
                }

                ArrayList<String> checked = this.sv.checkLogin(username, password);




            } catch (IOException e) {
                System.out.println("[EXCEPTION] IOException");
                e.printStackTrace();
                return;
            }
        }
    }

    private void serverErrorHandling() {
        System.out.println("[CLIENT] Server is down, trying to reconnect...");
        while (true) {
            try {
                Thread.sleep(keepAliveTime);
                this.sv = (ServerInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegistryName);
                this.sv.updateClient(this.client.username, this.client);
                break;
            } catch (RemoteException | NotBoundException | InterruptedException e) {
                System.out.println("[CLIENT] Server connection Failed... See error below:");
                e.printStackTrace();
                return;
            }
        }
    }

    @Override
    public void notifyMessage(String message) throws RemoteException {
        System.out.println(message);
    }
}
