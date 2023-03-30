package SearchEngine;

import Client.User;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Semaphore;

public class Database implements Serializable {
    private final int BUFFER_SIZE = 30; // buffer size
    Semaphore s_linksFile = new Semaphore(1);
    Semaphore s_linksInfoFile = new Semaphore(1);
    Semaphore s_wordsFile = new Semaphore(1);
    Semaphore s_usersFile = new Semaphore(1);
    private File usersFile;
    private int svID;


    public Database(int svID) {
        this.usersFile = new File("src\\users");
        this.svID = svID;
    }

    public HashMap<String, User> getUsers() {
        HashMap<String, User> users = new HashMap<>();
        try {
            this.s_usersFile.acquire();
            if (!this.usersFile.exists()) {
                this.usersFile.createNewFile();
                this.s_usersFile.release();
                updateUsers(new HashMap<String, User>());
                this.s_usersFile.acquire();
            } else {
                FileInputStream fis = new FileInputStream(this.usersFile);
                ObjectInputStream ois = new ObjectInputStream(fis);
                users = (HashMap<String, User>) ois.readObject();
                ois.close();
            }
            this.s_usersFile.release();
        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            System.out.println("[EXCEPTION] While getting users: " + e.getMessage());
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
            System.out.println("[EXCEPTION] While updating users: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateLinks(HashMap<String, HashSet<String>> fileLinks, File linksFile) {
        try {
            this.s_linksFile.acquire();
            if (!linksFile.exists()) {
                linksFile.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(linksFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            ArrayList<HashMap<String, HashSet<String>>> bufferList = new ArrayList<>();
            bufferList.add(fileLinks);
            if (bufferList.size() >= BUFFER_SIZE) {
                for (HashMap<String, HashSet<String>> bufferedFileLinks : bufferList) {
                    oos.writeObject(bufferedFileLinks);
                }
                bufferList.clear();
            }

            oos.close();
            fos.close();
            this.s_linksFile.release();
        } catch (IOException | InterruptedException e) {
            System.out.println("[EXCEPTION] While updating links: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateWords(HashMap<String, HashSet<String>> fileWords, File wordsFile) {
        try {
            this.s_wordsFile.acquire();
            if (!wordsFile.exists()) {
                wordsFile.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(wordsFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(fileWords);
            oos.close();
            fos.close();
            this.s_wordsFile.release();
        } catch (IOException | InterruptedException e) {
            System.out.println("[EXCEPTION] While updating links: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateInfo(HashMap<String, ArrayList<String>> fileInfo, File infoFile) {
        try {
            this.s_linksInfoFile.acquire();
            if (!infoFile.exists()) {
                infoFile.createNewFile();
            }
            // escreve numa class "HashNap"
            FileOutputStream fos = new FileOutputStream(infoFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(fileInfo);
            oos.close();
            fos.close();
            this.s_linksInfoFile.release();
        } catch (IOException | InterruptedException e) {
            System.out.println("[EXCEPTION] While updating links: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // links - links.
    public HashMap<String, HashSet<String>> getLinks(File linksFile) {
        HashMap<String, HashSet<String>> links = new HashMap<>();
        try {
            this.s_linksFile.acquire();
            if (!linksFile.exists()) {
                linksFile.createNewFile();
                this.s_linksFile.release();
                updateLinks(new HashMap<String, HashSet<String>>(), linksFile);
                this.s_linksFile.acquire();
            } else {

                FileInputStream fis = new FileInputStream(linksFile);
                ObjectInputStream ois = new ObjectInputStream(fis);

                links = (HashMap<String, HashSet<String>>) ois.readObject();

                ois.close();
            }
            this.s_linksFile.release();
        } catch (StreamCorruptedException | EOFException e) {
            System.out.println("[EXCEPTION] EOF Error, corrupted file: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            System.out.println("[EXCEPTION] While getting links: " + e.getMessage());
            e.printStackTrace();
        }
        // System.out.println("getLinks: " + links);
        return links;
    }

    // links- phrases.
    public HashMap<String, ArrayList<String>> getLinksInfo(File infofile) {
        HashMap<String, ArrayList<String>> linksInfo = new HashMap<>();
        try {
            this.s_linksInfoFile.acquire();
            if (!infofile.exists()) {
                infofile.createNewFile();
                this.s_linksInfoFile.release();
                updateInfo(new HashMap<String, ArrayList<String>>(), infofile);
                this.s_linksInfoFile.acquire();
            } else {
                FileInputStream fis = new FileInputStream(infofile);
                ObjectInputStream ois = new ObjectInputStream(fis);

                linksInfo = (HashMap<String, ArrayList<String>>) ois.readObject();

                ois.close();
            }
            this.s_linksInfoFile.release();
        } catch (StreamCorruptedException | EOFException e) {
            System.out.println("[EXCEPTION] EOF Error, corrupted file: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            System.out.println("[EXCEPTION] While getting links: " + e.getMessage());
            e.printStackTrace();
        }
        // System.out.println("getLinksInfo: " + linksInfo);
        return linksInfo;
    }

    // single words -> links.
    public HashMap<String, HashSet<String>> getWords(File wordsfile) {
        HashMap<String, HashSet<String>> words = new HashMap<>();
        try {
            this.s_wordsFile.acquire();

            if (!wordsfile.exists()) {
                wordsfile.createNewFile();
                this.s_wordsFile.release();
                updateWords(new HashMap<String, HashSet<String>>(), wordsfile);
                this.s_wordsFile.acquire();
            } else {
                FileInputStream fis = new FileInputStream(wordsfile);
                ObjectInputStream ois = new ObjectInputStream(fis);

                words = (HashMap<String, HashSet<String>>) ois.readObject();

                ois.close();
            }
            this.s_wordsFile.release();
        } catch (StreamCorruptedException | EOFException e) {
            System.out.println("[EXCEPTION] EOF Error, corrupted file: " + e.getMessage());
            e.printStackTrace();

        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            System.out.println("[EXCEPTION] While getting links: " + e.getMessage());
            e.printStackTrace();
        }
        // System.out.println("getWords: " + words);
        return words;
    }

    public HashSet<String> getLinksAssciatedWord(String word, File wordsfile) {
        HashSet<String> links = new HashSet<String>();
        try {
            this.s_wordsFile.acquire();
            if (!wordsfile.exists()) {
                wordsfile.createNewFile();
                this.s_wordsFile.release();
                updateWords(new HashMap<String, HashSet<String>>(), wordsfile);
                this.s_wordsFile.acquire();
            } else {
                FileInputStream fis = new FileInputStream(wordsfile);
                ObjectInputStream ois = new ObjectInputStream(fis);

                HashMap<String, HashSet<String>> words = (HashMap<String, HashSet<String>>) ois.readObject();
                System.out.println("getLinksAssciatedWord: " + words);
                ois.close();
                links = words.get(word);
                System.out.println("getLinksAssciatedWord: " + links);
            }
            this.s_wordsFile.release();
        } catch (IOException | InterruptedException | ClassNotFoundException e) {

            e.printStackTrace();
        }
        return links;
    }

    public ArrayList<String> getLinkInfo(String link, File infofile) {
        ArrayList<String> info = new ArrayList<>();

        try {
            this.s_linksFile.acquire();
            if (!infofile.exists()) {
                infofile.createNewFile();
                this.s_linksFile.release();
                updateLinks(new HashMap<String, HashSet<String>>(), infofile);
                this.s_linksFile.acquire();
            } else {
                FileInputStream fis = new FileInputStream(infofile);
                ObjectInputStream ois = new ObjectInputStream(fis);

                HashMap<String, ArrayList<String>> linksInfo = (HashMap<String, ArrayList<String>>) ois.readObject();

                ois.close();
                info = linksInfo.get(link);
            }
            this.s_linksFile.release();
        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return info;
    }
}


