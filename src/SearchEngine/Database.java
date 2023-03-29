package SearchEngine;

import Client.User;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.concurrent.Semaphore;

public class Database implements Serializable {
    private File linksFile;
    Semaphore s_linksFile = new Semaphore(1);
    private File linksInfoFile;
    Semaphore s_linksInfoFile = new Semaphore(1);
    private File wordsFile;
    Semaphore s_wordsFile = new Semaphore(1);

    private File usersFile;
    Semaphore s_usersFile = new Semaphore(1);


    public Database(int svID) {
        setPath(svID);
        this.s_usersFile = new Semaphore(1);
    }

    public void postUser(HashMap<String, User> users) {
    }

    public void setPath(int n) {
        String usersPath = "src\\users";
        this.usersFile = new File(usersPath);
    }

    public HashMap<String, User> getUsers() {
        HashMap<String, User> users = new HashMap<>();
        try {
            this.s_usersFile.acquire();
            if (!this.usersFile.exists()) {
                this.usersFile.createNewFile();
                this.s_usersFile.release();
                updateUsers(new HashMap<String,User>());
                this.s_usersFile.acquire();
            }
            else {
                FileInputStream fis = new FileInputStream(this.usersFile);
                ObjectInputStream ois = new ObjectInputStream(fis);
                users = (HashMap<String,User>) ois.readObject();
                ois.close();
            }
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
    public void updateLinks(HashMap<String, HashSet<String>> fileLinks, File linksFile) {
        try {

            if (!linksFile.exists()) {
                linksFile.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(linksFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(fileLinks);
            oos.close();
            fos.close();

        } catch (IOException e) {
            System.out.println("[EXCEPTION] While updating links: "+ e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateWords(HashMap<String, HashSet<String>> fileWords, File wordsFile) {
        try {

            if (!wordsFile.exists()) {
                wordsFile.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(wordsFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(fileWords);
            oos.close();
            fos.close();

        } catch (IOException e) {
            System.out.println("[EXCEPTION] While updating links: "+ e.getMessage());
            e.printStackTrace();
        }
    }
    public void updateInfo(HashMap<String, ArrayList<String>> fileInfo, File infoFile) {
        try {
            if (!infoFile.exists()) {
                infoFile.createNewFile();
            }
            // escreve numa class "HashNap"
            FileOutputStream fos = new FileOutputStream(infoFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(fileInfo);
            oos.close();
            fos.close();

        } catch (IOException e) {
            System.out.println("[EXCEPTION] While updating links: "+ e.getMessage());
            e.printStackTrace();
        }
    }


    public HashMap<String, HashSet<String>> getLinks(File linksFile) {
        HashMap<String, HashSet<String>> links = new HashMap<>();
        try {

            if (!linksFile.exists()) {
                return links;
            }

            FileInputStream fis = new FileInputStream(linksFile);
            ObjectInputStream ois = new ObjectInputStream(fis);

            links = (HashMap<String, HashSet<String>>) ois.readObject();
            ois.close();
            fis.close();

        } catch (IOException |  ClassNotFoundException e) {
            System.out.println("[EXCEPTION] While getting links: "+ e.getMessage());
            e.printStackTrace();
        }
        return links;
    }

    public HashMap<String, ArrayList<String>> getLinksInfo(File infofile) {
        HashMap<String, ArrayList<String>> linksInfo = new HashMap<>();
        try {

            if (!infofile.exists()) {
                return linksInfo;
            }

            FileInputStream fis = new FileInputStream(infofile);
            ObjectInputStream ois = new ObjectInputStream(fis);

            linksInfo = (HashMap<String, ArrayList<String>>) ois.readObject();
            ois.close();
            fis.close();

        } catch (IOException |  ClassNotFoundException e) {
            System.out.println("[EXCEPTION] While getting links: "+ e.getMessage());
            e.printStackTrace();
        }
        return linksInfo;
    }

    public HashMap<String, HashSet<String>> getWords(File wordsfile) {
        HashMap<String, HashSet<String>> words = new HashMap<>();
        try {

            if (!wordsfile.exists()) {
                return words;
            }

            FileInputStream fis = new FileInputStream(wordsfile);
            ObjectInputStream ois = new ObjectInputStream(fis);

            words = (HashMap<String, HashSet<String>>) ois.readObject();
            ois.close();
            fis.close();

        } catch (IOException |  ClassNotFoundException e) {
            System.out.println("[EXCEPTION] While getting links: "+ e.getMessage());
            e.printStackTrace();
        }
        return words;
    }

}


