package SearchEngine;

import Client.User;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Semaphore;

public class Database implements Serializable {
    Semaphore s_linksFile = new Semaphore(1);
    Semaphore s_linksInfoFile = new Semaphore(1);
    Semaphore s_wordsFile = new Semaphore(1);
    Semaphore s_usersFile = new Semaphore(1);
    Semaphore s_searchesFiles = new Semaphore(1);
    private File usersFile;
    private File searchesFile;
    private int svID;


    public Database(int svID) {
        this.usersFile = new File("src\\users");
        this.searchesFile = new File("src\\searches");
        this.svID = svID;
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

    public void updateLinks(HashMap<String, HashSet<String>> fileLinks, File linksFile, File backup) {
        try {
            this.s_linksFile.acquire();
            if (!linksFile.exists()) {
                linksFile.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(linksFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(fileLinks);
            oos.close();
            fos.close();

            if (!backup.exists()) {
                backup.createNewFile();
            }

            fos = new FileOutputStream(backup);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(fileLinks);
            oos.close();
            fos.close();

            this.s_linksFile.release();
        } catch (IOException | InterruptedException e) {
            System.out.println("[EXCEPTION] While updating links: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateWords(HashMap<String, HashSet<String>> fileWords, File wordsFile, File backup) {
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

            fos = new FileOutputStream(backup);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(fileWords);
            oos.close();
            fos.close();

            this.s_wordsFile.release();
        } catch (IOException | InterruptedException e) {
            System.out.println("[EXCEPTION] While updating links: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateInfo(HashMap<String, ArrayList<String>> fileInfo, File infoFile, File backup) {
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

            fos = new FileOutputStream(backup);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(fileInfo);
            oos.close();
            fos.close();

            this.s_linksInfoFile.release();
        } catch (IOException | InterruptedException e) {
            System.out.println("[EXCEPTION] While updating links: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateTopWords(HashMap<String, Integer> topWords) {
        try {
            this.s_searchesFiles.acquire();
            if (!searchesFile.exists()) {
                searchesFile.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(searchesFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(topWords);
            oos.close();
            fos.close();

            this.s_searchesFiles.release();
        } catch (IOException | InterruptedException e) {
            System.out.println("[EXCEPTION] While updating top words: " + e.getMessage());
            e.printStackTrace();
        }
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

    public HashMap<String, Integer> getTopWords() {
        HashMap<String, Integer> words = new HashMap<>();
        try {
            this.s_searchesFiles.acquire();
            if (!this.searchesFile.exists()) {
                this.searchesFile.createNewFile();
                this.s_searchesFiles.release();
                updateTopWords(new HashMap<String, Integer>());
                this.s_searchesFiles.acquire();
            } else {
                FileInputStream fis = new FileInputStream(this.searchesFile);
                ObjectInputStream ois = new ObjectInputStream(fis);
                words = (HashMap<String, Integer>) ois.readObject();
                ois.close();
            }
            this.s_searchesFiles.release();
        } catch (InterruptedException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return words;
    }

    // links - links.
    public HashMap<String, HashSet<String>> getLinks(File linksFile, File backup) {
        HashMap<String, HashSet<String>> links = new HashMap<>();
        try {
            this.s_linksFile.acquire();
            if (!linksFile.exists()) {
                linksFile.createNewFile();
                this.s_linksFile.release();
                updateLinks(new HashMap<String, HashSet<String>>(), linksFile, backup);
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
            try {
                FileInputStream fis = new FileInputStream(backup);
                ObjectInputStream ois = new ObjectInputStream(fis);

                links = (HashMap<String, HashSet<String>>) ois.readObject();


            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            System.out.println("[EXCEPTION] While getting links: " + e.getMessage());

            try {
                FileInputStream fis = new FileInputStream(backup);
                ObjectInputStream ois = new ObjectInputStream(fis);

                links = (HashMap<String, HashSet<String>>) ois.readObject();


            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }

        }
        // System.out.println("getLinks: " + links);
        return links;
    }

    // links- phrases.
    public HashMap<String, ArrayList<String>> getLinksInfo(File infofile, File backup) {
        HashMap<String, ArrayList<String>> linksInfo = new HashMap<>();
        try {
            this.s_linksInfoFile.acquire();
            if (!infofile.exists()) {
                infofile.createNewFile();
                this.s_linksInfoFile.release();
                updateInfo(new HashMap<String, ArrayList<String>>(), infofile, backup);
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
            try {
                FileInputStream fis = new FileInputStream(backup);
                ObjectInputStream ois = new ObjectInputStream(fis);

                linksInfo = (HashMap<String, ArrayList<String>>) ois.readObject();
                ois.close();
                return linksInfo;

            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            System.out.println("[EXCEPTION] While getting info: " + e.getMessage());

            try {
                FileInputStream fis = new FileInputStream(backup);
                ObjectInputStream ois = new ObjectInputStream(fis);

                linksInfo = (HashMap<String, ArrayList<String>>) ois.readObject();
                ois.close();
                return linksInfo;

            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }


        }
        // System.out.println("getLinksInfo: " + linksInfo);
        return linksInfo;
    }

    // single words -> links.
    public HashMap<String, HashSet<String>> getWords(File wordsfile, File backup) {
        HashMap<String, HashSet<String>> words = new HashMap<>();
        try {
            this.s_wordsFile.acquire();

            if (!wordsfile.exists()) {
                wordsfile.createNewFile();
                this.s_wordsFile.release();
                updateWords(new HashMap<String, HashSet<String>>(), wordsfile, backup);
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
            try {
                FileInputStream fis = new FileInputStream(backup);
                ObjectInputStream ois = new ObjectInputStream(fis);

                words = (HashMap<String, HashSet<String>>) ois.readObject();

                ois.close();

                return words;
            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }

        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            System.out.println("[EXCEPTION] While getting words: " + e.getMessage());
            try {
                FileInputStream fis = new FileInputStream(backup);
                ObjectInputStream ois = new ObjectInputStream(fis);

                words = (HashMap<String, HashSet<String>>) ois.readObject();

                ois.close();

                return words;
            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }

        }
        // System.out.println("getWords: " + words);
        return words;
    }

}


