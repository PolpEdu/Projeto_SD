package SearchEngine;

import Client.User;

import java.io.*;
import java.util.HashMap;

public class Database {
    private File linksFile;
    private File wordsFile;
    private File linksInfoFile;

    private File usersFile;
    private File wordCountFile;

    public void constructPaths(int n) {
        String usersPath = "src\\Users" + n + ".txt";

        this.usersFile = new File(usersPath);

    }

    public Database(int svID) {
        setPath(svID);


    }

    public void postUser(HashMap<String, User> users) {
        // there will be plenty of threads trying to write to the file make this code synchronized
        synchronized (this) {

        }
    }

    public void setPath(int n) {
        String linksPath = "src\\links" + n + ".txt";
        String wordsPath = "src\\words" + n + ".txt";
        String linksInfoPath = "src\\linksInfo" + n + ".txt";
        String usersPath = "src\\users" + n + ".txt";
        String wordCountPath = "src\\wordCount" + n + ".txt";

        this.usersFile = new File(usersPath);
        this.linksFile = new File(linksPath);
        this.wordsFile = new File(wordsPath);
        this.linksInfoFile = new File(linksInfoPath);
        this.wordCountFile = new File(wordCountPath);
    }

    public HashMap<String, User> getUsers() {
        //throw new Exception("Not implemented yet fdp.");
        HashMap<String, User> users = new HashMap<>();
        return users;
    }
}


