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

    private final ServerInterface serverInterface;
    private Client client;

    public RMIClient(ServerInterface svInterface, Client client) throws RemoteException{
        super();
        this.serverInterface = svInterface;
        this.client = client;
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
                System.out.println("Current config: " + rmiHost + ":" + rmiPort + " " + rmiRegistryName);
                return;
            }

            System.out.println("[CLIENT] Running on " + rmiHost + ":" + rmiPort);


            // GET SERVER INTERFACE USING REGISTRY
            ServerInterface svInterface = (ServerInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegistryName);


            Client client = new Client("Anon", false);
            RMIClient rmi_client = new RMIClient(svInterface, client);
            rmi_client.menu();

        } catch (RemoteException e){
            System.out.println("[EXCEPTION] RemoteException");
            e.printStackTrace();
            return;
        } catch (IOException e) {
            System.out.println("[EXCEPTION] IOException");
            e.printStackTrace();
            return;
        } catch (NotBoundException e) {
            System.out.println("[EXCEPTION] NotBoundException");
            e.printStackTrace();
            return;
        }
    }

    private void printMenus(int type) {

        switch (type) {
            case 0:
                // register type
                System.out.print("\n###MENU###\n1.Login\n2.Register\n 3.Search words\n4.Search Link\ne.Exit\n -> Choice: ");
                break;
            case 1:
                if (this.client.admin) {
                    // admin menu
                    return;
                }


        }

    }

    private void menu() {
        InputStream in  = System.in;
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        String choice = "";
        boolean end = false;

        // create a new client object
        this.client = new Client("Anon", false);



        while (true) {
            // anonymous user, not logged yet
            if (this.client.username.equals("Anon")) {
                printMenus(0);
            } else {
                printMenus(1);
            }

        }

    }


    @Override
    public void notifyMessage(String message) throws RemoteException {
        System.out.println(message);
    }
}
