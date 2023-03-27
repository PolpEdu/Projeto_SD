package SearchEngine;

import Client.User;
import Utility.MessageUpdateInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class TCPServer extends Thread {
    private ServerSocket listenSocket;
    private int PORT;
    private Database fileManager;

    public TCPServer(int port, Database fileManager) {
        this.PORT = selectPort(port);
        this.fileManager = fileManager;

        System.out.println("[TCPServer-" + this.getName() + "] Running on PORT " + this.PORT + ".");
        this.start();
    }

    private int selectPort(int port) {
        while (!isPortAvailable(port)) {
            port++;
        }
        return port;
    }

    private boolean isPortAvailable(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int getPORT() {
        return PORT;
    }

    public void run() {
        try {
            this.listenSocket = new ServerSocket(this.PORT);
            while (true) {
                Socket clientSocket = this.listenSocket.accept();
                System.out.println("[TCPConnection-" + this.getName() + "] New connection from " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
                new Updates(clientSocket, this.fileManager);
            }
        } catch (Exception e) {
            System.out.println("[EXCEPTION-" + this.getName() + "] Error: " + e.getMessage());
        }
    }
}

// class to receive updates from other servers
class Updates extends Thread {
    private Socket clientSocket;
    private Database fileManager;

    // use object streaming to receive the message
    private ObjectInputStream inp;

    public Updates(Socket clientSocket, Database fileManager) {
        this.clientSocket = clientSocket;
        this.fileManager = fileManager;

        try {
            this.inp = new ObjectInputStream(this.clientSocket.getInputStream());
        } catch (Exception e) {
            System.out.println("[EXCEPTION-" + this.getName() + "] Error: " + e.getMessage());
            return;
        }

        this.start();
    }

    public void run() {
        try {
            MessageUpdateInfo message = (MessageUpdateInfo) this.inp.readObject();
            System.out.println("[TCPConnection-" + this.getName() + "] Received message from " + message.PORT);
            this.handleData(message);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("[EXCEPTION-" + this.getName() + "] Class not found: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleData(MessageUpdateInfo message) {
        HashMap<String, User> users = message.getUsers();
        HashMap<String, User> fileUsers = this.fileManager.getUsers();

        HashMap<String, ArrayList<String>> links = message.getLinks();
        HashMap<String, ArrayList<String>> fileLinks = this.fileManager.getLinks();

        HashMap<String, ArrayList<String>> linksInfo = message.getLinksInfo();
        HashMap<String, ArrayList<String>> fileLinksInfo = this.fileManager.getLinksInfo();

        HashMap<String, ArrayList<String>> words = message.getWords();
        HashMap<String, ArrayList<String>> fileWords = this.fileManager.getWords();

        HashMap<String, Integer> wordsCount = message.getWordsCount();
        HashMap<String, Integer> fileWordsCount = this.fileManager.getWordsCount();

        // update the data in the files
        this.updateUsers(users, fileUsers);
        this.updateLinks(links, fileLinks);
        /*this.updateLinksInfo(linksInfo, fileLinksInfo);
        this.updateWords(words, fileWords);
        this.updateWordsCount(wordsCount, fileWordsCount);*/


    }

    private void updateUsers(HashMap<String, User> users, HashMap<String, User> fileUsers) {
        for (String username : users.keySet()) {
            fileUsers.put(username, users.get(username));
        }
        // update the file
        this.fileManager.updateUsers(fileUsers);
    }

    private void updateLinks(HashMap<String, ArrayList<String>> links, HashMap<String, ArrayList<String>> fileLinks) {
        for (String link : links.keySet()) {
            if (!fileLinks.containsKey(link)) {
                fileLinks.put(link, new ArrayList<>());
            }
            // add the references
            for (String ref : links.get(link)) {
                fileLinks.get(link).add(ref);
            }
        }
        // update the file
        this.fileManager.updateLinks(fileLinks);
    }


}
