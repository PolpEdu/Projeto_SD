package com.ProjetoSD.SearchEngine;

import com.ProjetoSD.interfaces.RMIUrlQueueInterface;

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
/**
 * UrlQueue class that implements the RMIUrlQueueInterface.
 */
public class UrlQueue extends UnicastRemoteObject implements RMIUrlQueueInterface {

    /**
     * queue of urls to be indexed
     */
    private final LinkedBlockingQueue<String> urlQueue;
    /**
     * file to save the queue
     */
    private final File urlqueuefile;
    /**
     * file to save the queue - backup
     */
    private final File urlqueuefileb;
    /**
     * database object to access the write and read methods
     */
    private final Database db;

    /**
     * Constructor of the UrlQueue class.
     * here we initialize the files to read and write, the database object and the queue.
     * @throws RemoteException
     */
    public UrlQueue() throws RemoteException {
        super();
        this.db = new Database();

        this.urlqueuefile = new File("src/main/java/com/ProjetoSD/urlqueue");
        this.urlqueuefileb = new File("src/main/java/com/ProjetoSD/urlqueueb");
        this.urlQueue = db.getUrlQueue(this.urlqueuefile, this.urlqueuefileb);

//        if(this.urlQueue.isEmpty()){
//            this.urlQueue.offer("https://www.uc.pt");
//        }

    }

    /**
     * main method to start the UrlQueue and de RMI server
     * @param args
     */
    public static void main(String[] args) {
        System.getProperties().put("java.security.policy", "policy.all");
        Properties prop = new Properties();

        String SETTINGS_PATH = "src/main/java/com/ProjetoSD/URLQueue.properties";

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

    /**
     * function that a downloader will call to get a link from the queue
     * @return link
     * @throws RemoteException
     */
    @Override
    public String takeLink() throws RemoteException {

        try {
            return this.urlQueue.take();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * function that a crawler will call to add a link to the queue
     * @param link link to be added
     * @throws RemoteException
     */
    @Override
    public void offerLink(String link) throws RemoteException {
        this.urlQueue.offer(link);
        this.db.updateUrlQueue(this.urlQueue, this.urlqueuefile, this.urlqueuefileb);
    }

    /**
     * function that a crawler will call to check if the queue is empty
     * @return true if the queue is empty, false otherwise
     * @throws RemoteException
     */
    @Override
    public boolean isempty() throws RemoteException {
        return this.urlQueue.isEmpty();
    }
}
