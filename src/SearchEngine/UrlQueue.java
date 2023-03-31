package SearchEngine;

import interfaces.RMIUrlQueueInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

public class UrlQueue extends UnicastRemoteObject implements RMIUrlQueueInterface {
    private final LinkedBlockingQueue<String> urlQueue;
    private final File urlqueuefile;
    private final File urlqueuefileb;

    private final Database db;

    public UrlQueue() throws RemoteException {
        super();
        this.db = new Database(1);

        this.urlqueuefile = new File("src\\urlqueue");
        this.urlqueuefileb = new File("src\\urlqueueb");
        this.urlQueue = db.getUrlQueue(this.urlqueuefile, this.urlqueuefileb);

//        if(this.urlQueue.isEmpty()){
//            this.urlQueue.offer("https://www.uc.pt");
//        }

    }

    public static void main(String[] args) {
        System.getProperties().put("java.security.policy", "policy.all");
        Properties prop = new Properties();

        String SETTINGS_PATH = "src\\URLQueue.properties";

        String rmiRegistryName;
        String rmiHost;
        int rmiPort;
        UrlQueue urlQueue;

        try {
            prop.load(new FileInputStream(SETTINGS_PATH));
            rmiHost = prop.getProperty("RMI_HOST");
            rmiPort = Integer.parseInt(prop.getProperty("RMI_PORT"));

            rmiRegistryName = prop.getProperty("RMI_REGISTRY_NAME");
            urlQueue = new UrlQueue();
            Registry r = LocateRegistry.createRegistry(rmiPort);


            System.setProperty("java.rmi.server.hostname", rmiHost); // set the host name
            r.rebind(rmiRegistryName, urlQueue);
            System.out.println("[URLQUEUE] Running on " + rmiHost + ":" + rmiPort + "->" + rmiRegistryName);


        }
        catch (RemoteException er)
        {
            er.printStackTrace();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        this.db.updateUrlQueue(this.urlQueue, this.urlqueuefile, this.urlqueuefileb);
    }

    @Override
    public boolean isempty() throws RemoteException {
        return this.urlQueue.isEmpty();
    }
}
