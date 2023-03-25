package SearchEngine;

import Client.User;

import java.io.*;
import java.util.HashMap;

public class Database {
    private File linksFile;
    private File wordsFile;
    private File linksInfoFile;

    private File usersFile;

    public void constructPaths(int n) {
        String usersPath = "src\\Users" + n + ".txt";

        this.usersFile = new File(usersPath);

    }

    public Database() {


    }

    public void postUser(HashMap<String, User> users) {
        // there will be plenty of threads trying to write to the file make this code synchronized
        synchronized (this) {

        }
    }
}


