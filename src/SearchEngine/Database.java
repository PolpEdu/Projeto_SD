package SearchEngine;

import Client.User;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class Database {
    private File linksFile;
    Semaphore s_linksFile = new Semaphore(1);
    private File linksInfoFile;
    Semaphore s_linksInfoFile = new Semaphore(1);
    private File wordsFile;
    Semaphore s_wordsFile = new Semaphore(1);

    private File usersFile;
    Semaphore s_usersFile = new Semaphore(1);


    public void constructPaths(int n) {
        String usersPath = "src\\Users" + n + ".txt";

        this.usersFile = new File(usersPath);

    }

    public Database(int svID) {
        setPath(svID);
        this.s_usersFile = new Semaphore(1);
        this.s_linksFile = new Semaphore(1);
        this.s_linksInfoFile = new Semaphore(1);
        this.s_wordsFile = new Semaphore(1);
    }

    public void postUser(HashMap<String, User> users) {
    }

    public void setPath(int n) {
        String linksPath = "src\\links" + n + ".txt";
        String wordsPath = "src\\words" + n + ".txt";
        String linksInfoPath = "src\\linksInfo" + n + ".txt";
        String usersPath = "src\\users" + n + ".txt";

        this.usersFile = new File(usersPath);
        this.linksFile = new File(linksPath);
        this.wordsFile = new File(wordsPath);
        this.linksInfoFile = new File(linksInfoPath);
    }

    public HashMap<String, User> getUsers() {
        HashMap<String, User> users = new HashMap<>();
        try {
            this.s_usersFile.acquire();
            if (!this.usersFile.exists()) {
                this.usersFile.createNewFile();
                this.s_usersFile.release(); // stop semaphore to update file with no users
                updateUsers(users); // empty
                this.s_usersFile.acquire(); // start semaphore again
            }

            FileInputStream fis = new FileInputStream(this.usersFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            users = (HashMap<String, User>) ois.readObject();
            ois.close();
            fis.close();
            this.s_usersFile.release();
        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            System.out.println("[EXCEPTION] While getting users: "+ e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    public void updateUsers(HashMap<String, User> users) {
        try {
            this.s_usersFile.acquire();
            if (!this.usersFile.exists()) {
                this.usersFile.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(this.usersFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(users);
            oos.close();
            fos.close();
            this.s_usersFile.release();
        } catch (InterruptedException | IOException e) {
            System.out.println("[EXCEPTION] While updating users: "+ e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateLinks(HashMap<String, ArrayList<String>> fileLinks) {
        try {
            this.s_linksFile.acquire();
            if (!this.linksFile.exists()) {
                this.linksFile.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(this.linksFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(fileLinks);
            oos.close();
            fos.close();
            this.s_linksFile.release();
        } catch (InterruptedException | IOException e) {
            System.out.println("[EXCEPTION] While updating links: "+ e.getMessage());
            e.printStackTrace();
        }
    }

    public HashMap<String, ArrayList<String>> getLinks() {
        HashMap<String, ArrayList<String>> links = new HashMap<>();
        return links;
    }

    public HashMap<String, ArrayList<String>> getLinksInfo() {
        HashMap<String, ArrayList<String>> linksInfo = new HashMap<>();
        return linksInfo;
    }

    public HashMap<String, ArrayList<String>> getWords() {
        HashMap<String, ArrayList<String>> words = new HashMap<>();
        return words;
    }

}


