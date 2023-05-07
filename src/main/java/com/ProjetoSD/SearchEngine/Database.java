package com.ProjetoSD.SearchEngine;

import com.ProjetoSD.Client.User;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * Class that represents the database of the server. It stores the users, the searches, the links, the words and the
 */
public class Database implements Serializable {
    /**
     * semophore to control the access to the links file
     */
    Semaphore s_linksFile = new Semaphore(1);
    /**
     * semophore to control the access to the info file
     */
    Semaphore s_linksInfoFile = new Semaphore(1);
    /**
     * semophore to control the access to the words file
     */
    Semaphore s_wordsFile = new Semaphore(1);
    /**
     * semophore to control the access to the users file
     */
    Semaphore s_usersFile = new Semaphore(1);
    /**
     * semophore to control the access to the searchers file
     */
    Semaphore s_searchesFiles = new Semaphore(1);
    /**
     * usersfile
     */
    private File usersFile;
    /**
     * Searchers file
     */
    private File searchesFile;

    /**
     * Construtor da classe Database. Here we initialize the files that are not barrel thread dependent,
     * in other words, since we'll have just a users file and a searches file that will be used to store the data
     * of all the barrels, we don't need to create a new file for each barrel.
     */
    public Database() {
        this.usersFile = new File("src/main/java/com/ProjetoSD/users");
        this.searchesFile = new File("src/main/java/com/ProjetoSD/searches");
    }

    /**
     * Method that updates the users file. It takes in a HashMap of users and writes it to the file.
     * It also uses a semaphore to control the access to the file. 
     * @param users HashMap of users to be written to the file.
     */
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

    /**
     * Method that updates the links file. It takes in a HashMap of links and writes it to the file.
     * This method also uses a semaphore to control the access to the file. It also updates a backup file
     * whenever it updates the links file to be used in case of a crash, we'll always have the last updated
     * version of the file. Without the backup file, we would lose the data by corruption that was not written to the file
     * (Usually we use the back-up when we close the program in a middle of a write operation, that corrupts the file)
     * 
     * @param fileLinks HashMap of linksto be written to the file.
     * @param linksFile File to be written to. Usually it depends on the barrel thread.
     * @param backup Backup file to be written to.
     */
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

    /**
     * Method that updates the words file. It takes in a HashMap of words and writes it to the file.
     * This method also uses a semaphore to control the access to the file. It also updates a backup file
     * whenever it updates the words file to be used in case of a crash, we'll always have the last updated
     * version of the file. Without the backup file, we would lose the data by corruption that was not written to the file
     * (Usually we use the back-up when we close the program in a middle of a write operation, that corrupts the file)
     * @param fileWords HashMap of words to be written to the file.
     * @param wordsFile File to be written to. Usually it depends on the barrel thread.
     * @param backup Backup file to be written to.
     */
    public void updateWords(HashMap<String, HashSet<String>> fileWords, File wordsFile, File backup) {
        try {
            this.s_wordsFile.acquire();
            if (!wordsFile.exists()) {
                wordsFile.createNewFile();
            }
            if (!backup.exists()) {
                backup.createNewFile();
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

    /**
     * Method that updates the Information about a link file (the file that stores the small quotes about a link).
     * It takes in a HashMap of information and writes it to the file. This method also uses a semaphore to control the access to the file.
     * It also updates a backup file whenever it updates the information file to be used in case of a crash, we'll always have the last updated
     * version of the file. Without the backup file, we would lose the data by corruption that was not written to the file
     * (Usually we use the back-up when we close the program in a middle of a write operation, that corrupts it)
     * @param fileInfo HashMap of information to be written to the file.
     * @param infoFile File to be written to. Usually it depends on the barrel thread.
     * @param backup Backup file to be written to.
     */
    public void updateInfo(HashMap<String, ArrayList<String>> fileInfo, File infoFile, File backup) {
        try {
            this.s_linksInfoFile.acquire();
            if (!infoFile.exists()) {
                infoFile.createNewFile();
            }

            if (!backup.exists()) {
                backup.createNewFile();
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

    /**
     * Method that updates the top searches file. It takes in a HashMap of top searches and writes it to the file.
     * This method also uses a semaphore to control the access to the file and doesn't use any backup because there's
     * no constant writing to this file. Only occasionally, when the user searches for a word.
     * @param topWords HashMap of top searches to be written to the file.
     */
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

    /**
     * Method that gets all the users from the users file. This method returns a HashMap of users .
     * This method also uses a semaphore to control the access to the file. If the file doesn't exist, it creates a new one.
     * @return HashMap of users stored .
     */
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

    /**
     * Method that gets all the number of times a phrase was searched. This method returns a HashMap of top searches .
     * This method also uses a semaphore to control the access to the file. If the file doesn't exist, it creates a new one.
     * @return HashMap of searches .
     */
    public HashMap<String, Integer> getTop10Searches() {
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

    /**
     * Method that gets all the links from the links file. This method returns a HashMap of links.
     * If it is unable to read the file, it will try to read the backup file. As mentioned above, we update to two files 
     * simultaneously, so if one is corrupted, we can still read the other. This is our fail-over mechanism.
     * This method also uses a semaphore to control the access to the file. If the file doesn't exist, it creates a new one.
     * 
     * @param linksFile File to be read from.
     * @param backup Backup file to be read from if the main file is corrupted.
     * @return HashMap of links stored .
     */
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

                if(!backup.exists()){
                    backup.createNewFile();
                    updateWords(new HashMap<String, HashSet<String>>(), linksFile, backup);
                }
                else{
                    FileInputStream fis = new FileInputStream(backup);
                    ObjectInputStream ois = new ObjectInputStream(fis);

                    links = (HashMap<String, HashSet<String>>) ois.readObject();

                    ois.close();

                }
                return  links;
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
                if(!backup.exists()){
                    backup.createNewFile();
                    updateWords(new HashMap<String, HashSet<String>>(), linksFile, backup);
                }
                else{
                    FileInputStream fis = new FileInputStream(backup);
                    ObjectInputStream ois = new ObjectInputStream(fis);

                    links = (HashMap<String, HashSet<String>>) ois.readObject();

                    ois.close();

                }
                return links;

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

    /**
     * Method that gets all the links from the links file. This method returns a HashMap of links .
     * If it is unable to read the file, it will try to read the backup file. As mentioned above, we update to two files
     * simultaneously, so if one is corrupted, we can still read the other. This is our fail-over mechanism.
     * This method also uses a semaphore to control the access to the file. If the file doesn't exist, it creates a new one.
     * @param infofile Main file to be read from.
     * @param backup Backup file to be read from if the main file is corrupted.
     * @return HashMap of links stored .
     */
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
                if(!backup.exists()){
                    backup.createNewFile();
                    updateWords(new HashMap<String, HashSet<String>>(), infofile, backup);
                }
                else{
                    FileInputStream fis = new FileInputStream(backup);
                    ObjectInputStream ois = new ObjectInputStream(fis);

                    linksInfo = (HashMap<String, ArrayList<String>>) ois.readObject();
                    ois.close();
                }

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
                if(!backup.exists()){
                    backup.createNewFile();
                    updateWords(new HashMap<String, HashSet<String>>(), infofile, backup);
                }
                else{
                    FileInputStream fis = new FileInputStream(backup);
                    ObjectInputStream ois = new ObjectInputStream(fis);

                    linksInfo = (HashMap<String, ArrayList<String>>) ois.readObject();
                    ois.close();
                }

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

    /**
     * Method that gets all the links associated with a word from the words file. This method returns a HashMap of words .
     * If it is unable to read the file, it will try to read the backup file. As mentioned above, we update to two files
     * simultaneously, so if one is corrupted, we can still read the other. This is our fail-over mechanism.
     * This method also uses a semaphore to control the access to the file. If the file doesn't exist, it creates a new one.
     * @param wordsfile Main file to be read from.
     * @param backup Backup file to be read from if the main file is corrupted.
     * @return HashMap of words stored .
     */
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
                if(!backup.exists()){
                    backup.createNewFile();
                    updateWords(new HashMap<String, HashSet<String>>(), wordsfile, backup);
                }
                else{
                    FileInputStream fis = new FileInputStream(backup);
                    ObjectInputStream ois = new ObjectInputStream(fis);

                    words = (HashMap<String, HashSet<String>>) ois.readObject();

                    ois.close();
                }


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
                if(!backup.exists()){
                    backup.createNewFile();
                    updateWords(new HashMap<String, HashSet<String>>(), wordsfile, backup);
                }
                else{
                    FileInputStream fis = new FileInputStream(backup);
                    ObjectInputStream ois = new ObjectInputStream(fis);

                    words = (HashMap<String, HashSet<String>>) ois.readObject();

                    ois.close();
                }


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

    /**
     * Method that gets the stored url queue from the urlQueue file. This method returns a LinkedBlockingQueue of urls.
     * If it is unable to read the file, it will try to read the url queue backup file. As mentioned above, we update to
     * two files simultaneously, so if one is corrupted, we can still read the other. This is our fail-over mechanism.
     * This method also uses a semaphore to control the access to the file. If the file doesn't exist, it creates a new one.
     * @param urlQueue Main file to be read from.
     * @param backup Backup file to be read from if the main file is corrupted.
     * @return LinkedBlockingQueue of urls stored.
     */
    public LinkedBlockingQueue<String> getUrlQueue(File urlQueue, File backup) {
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        try {

            if (!urlQueue.exists()) {
                urlQueue.createNewFile();

                updateUrlQueue(new LinkedBlockingQueue<>(), urlQueue, backup);

            } else {
                FileInputStream fis = new FileInputStream(urlQueue);
                ObjectInputStream ois = new ObjectInputStream(fis);

                queue = (LinkedBlockingQueue<String>) ois.readObject();

                ois.close();
            }

        } catch (StreamCorruptedException | EOFException e) {
            System.out.println("[EXCEPTION] EOF Error, corrupted file: " + e.getMessage());
            try {
                FileInputStream fis = new FileInputStream(backup);
                ObjectInputStream ois = new ObjectInputStream(fis);

                queue = (LinkedBlockingQueue<String>) ois.readObject();
                ois.close();
                return queue;

            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[EXCEPTION] While getting info: " + e.getMessage());

            try {
                FileInputStream fis = new FileInputStream(backup);
                ObjectInputStream ois = new ObjectInputStream(fis);

                queue = (LinkedBlockingQueue<String>) ois.readObject();
                ois.close();
                return queue;

            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }


        }
        // System.out.println("getLinksInfo: " + linksInfo);
        return queue;
    }

    /**
     * Method that updates the UrlQueue file. This method uses a semaphore to control the access to the file.
     * If the file doesn't exist, it creates a new one. This method also updates the backup file to the same content because
     * if the program fails while updating the main file it corrupts it so we created a backup file to be used as a fail-over
     * mechanism.
     * @param uqueue LinkedBlockingQueue of urls to be stored.
     * @param urlqueue Main file to be updated.
     * @param backup Backup file to be updated.
     */
    public void updateUrlQueue(LinkedBlockingQueue<String> uqueue, File urlqueue, File backup) {
        try {

            if (!urlqueue.exists()) {
                urlqueue.createNewFile();
            }
            // escreve numa class "HashNap"
            FileOutputStream fos = new FileOutputStream(urlqueue);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(uqueue);
            oos.close();
            fos.close();

            fos = new FileOutputStream(backup);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(uqueue);
            oos.close();
            fos.close();


        } catch (IOException e) {
            System.out.println("[EXCEPTION] While updating links: " + e.getMessage());
            e.printStackTrace();
        }
    }


}


