package SearchEngine;

import Client.User;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.concurrent.Semaphore;

public class Database implements Serializable {
    Semaphore s_linksFile = new Semaphore(1);
    Semaphore s_linksInfoFile = new Semaphore(1);
    Semaphore s_wordsFile = new Semaphore(1);

    private File usersFile;
    Semaphore s_usersFile = new Semaphore(1);


    public Database(int svID) {
        setPath(svID);
        this.s_usersFile = new Semaphore(1);
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
    public void updateLinks(HashMap<String, HashSet<String>> fileLinks, File linksFile, File backup) {
        HashMap<String, HashSet<String>> links;
        try {
            if (!linksFile.exists()) {
                linksFile.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(linksFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(fileLinks);
            oos.close();
            fos.close();

            FileInputStream fis = new FileInputStream(linksFile);
            ObjectInputStream ois = new ObjectInputStream(fis);

            links = (HashMap<String, HashSet<String>>) ois.readObject();

            if (!backup.exists()){
                backup.createNewFile();
            }
            if(links.size() != 0){
                fos = new FileOutputStream(backup);
                oos = new ObjectOutputStream(fos);
                oos.writeObject(links);
                oos.close();
                fos.close();
            }

            ois.close();

        } catch (IOException e) {
            System.out.println("[EXCEPTION] While updating links: "+ e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateWords(HashMap<String, HashSet<String>> fileWords, File wordsFile, File backup) {
        HashMap<String, HashSet<String>> words;
        try {
            if (!wordsFile.exists()) {
                wordsFile.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(wordsFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(fileWords);
            oos.close();
            fos.close();

            FileInputStream fis = new FileInputStream(wordsFile);
            ObjectInputStream ois = new ObjectInputStream(fis);

            words = (HashMap<String, HashSet<String>>) ois.readObject();

            if (!backup.exists()){
                backup.createNewFile();
            }

            if(words.size() != 0){
                fos = new FileOutputStream(backup);
                oos = new ObjectOutputStream(fos);
                oos.writeObject(words);
                oos.close();
                fos.close();
            }

            ois.close();

        } catch (IOException e) {
            System.out.println("[EXCEPTION] While updating links: "+ e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    public void updateInfo(HashMap<String, ArrayList<String>> fileInfo, File infoFile, File backup) {
        HashMap<String, ArrayList<String>> info;
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

            FileInputStream fis = new FileInputStream(infoFile);
            ObjectInputStream ois = new ObjectInputStream(fis);

            info = (HashMap<String, ArrayList<String>>) ois.readObject();

            if (!backup.exists()){
                backup.createNewFile();
            }
            if(info.size() != 0){
                fos = new FileOutputStream(backup);
                oos = new ObjectOutputStream(fos);
                oos.writeObject(info);
                oos.close();
                fos.close();
            }

            ois.close();

        } catch (IOException e) {
            System.out.println("[EXCEPTION] While updating links: "+ e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public HashMap<String, HashSet<String>> getLinks(File linksFile, File backup) {
        HashMap<String, HashSet<String>> links = new HashMap<>();
        try {
            this.s_linksFile.acquire();
            if (!linksFile.exists()) {
                linksFile.createNewFile();
                this.s_linksFile.release();
                updateLinks(new HashMap<String,HashSet<String>>(), linksFile, backup);
                this.s_linksFile.acquire();
            } else {

                FileInputStream fis = new FileInputStream(linksFile);
                ObjectInputStream ois = new ObjectInputStream(fis);

                links = (HashMap<String, HashSet<String>>) ois.readObject();
                if(links.size() == 0){
                    fis = new FileInputStream(backup);
                    ois = new ObjectInputStream(fis);
                    links = (HashMap<String, HashSet<String>>) ois.readObject();
                    ois.close();
                }
                ois.close();
            }
            this.s_linksFile.release();
        }catch (StreamCorruptedException e) {
            System.out.println("[EXCEPTION] EOF Error, corrupted file: "+ e.getMessage());
            e.printStackTrace();
        }
        catch (IOException | ClassNotFoundException | InterruptedException e) {
            System.out.println("[EXCEPTION] While getting links: "+ e.getMessage());
            e.printStackTrace();
        }
        // System.out.println("links: " + links);
        return links;
    }

    public HashMap<String, ArrayList<String>> getLinksInfo(File infofile, File backup) {
        HashMap<String, ArrayList<String>> linksInfo = new HashMap<>();
        try {
            this.s_linksFile.acquire();
            if (!infofile.exists()) {
                infofile.createNewFile();
                this.s_linksFile.release();
                updateInfo(new HashMap<String,ArrayList<String>>(), infofile, backup);
                this.s_linksFile.acquire();
            } else {
                FileInputStream fis = new FileInputStream(infofile);
                ObjectInputStream ois = new ObjectInputStream(fis);

                linksInfo = (HashMap<String, ArrayList<String>>) ois.readObject();
                if(linksInfo.size() == 0){
                    fis = new FileInputStream(backup);
                    ois = new ObjectInputStream(fis);
                    linksInfo = (HashMap<String, ArrayList<String>>) ois.readObject();
                    ois.close();
                }
                ois.close();
            }
            this.s_linksFile.release();
        }catch (StreamCorruptedException e) {
            System.out.println("[EXCEPTION] EOF Error, corrupted file: "+ e.getMessage());
            e.printStackTrace();
        }
        catch (IOException | ClassNotFoundException | InterruptedException e) {
            System.out.println("[EXCEPTION] While getting links: "+ e.getMessage());
            e.printStackTrace();
        }
        // System.out.println("words: " + linksInfo);
        return linksInfo;
    }

    public HashMap<String, HashSet<String>> getWords(File wordsfile, File backup) {
        HashMap<String, HashSet<String>> words = new HashMap<>();
        try {
            this.s_wordsFile.acquire();

            if (!wordsfile.exists()) {
                wordsfile.createNewFile();
                this.s_wordsFile.release();
                updateWords(new HashMap<String,HashSet<String>>(), wordsfile, backup);
                this.s_wordsFile.acquire();
            } else {
                FileInputStream fis = new FileInputStream(wordsfile);
                ObjectInputStream ois = new ObjectInputStream(fis);

                words = (HashMap<String, HashSet<String>>) ois.readObject();
                if(words.size() == 0){
                    fis = new FileInputStream(backup);
                    ois = new ObjectInputStream(fis);
                    words = (HashMap<String, HashSet<String>>) ois.readObject();
                    ois.close();
                }
                ois.close();
            }
            this.s_wordsFile.release();
        } catch (StreamCorruptedException e) {
            System.out.println("[EXCEPTION] EOF Error, corrupted file: "+ e.getMessage());
            e.printStackTrace();
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            System.out.println("[EXCEPTION] While getting links: "+ e.getMessage());
            e.printStackTrace();
        }
        // System.out.println("words: " + words);
        return words;
    }

}


