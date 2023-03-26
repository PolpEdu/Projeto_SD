package SearchEngine;

import Utility.MessageUpdateInfo;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer extends Thread{
    private ServerSocket listenSocket;
    private int PORT;
    private Database fileManager;

    public TCPServer(int port, Database fileManager){
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

    public void run(){
        try{
            this.listenSocket = new ServerSocket(this.PORT);
            while(true){
                Socket clientSocket = this.listenSocket.accept();
                new Updates(clientSocket, this.PORT, this.fileManager);
            }
        }catch(Exception e){
            System.out.println("[EXCEPTION-" + this.getName() + "] Error: " + e.getMessage());
        }
    }
}

// class to receive updates from other servers
class Updates extends Thread {
    private Socket clientSocket;
    private Database fileManager;
    private int PORT;

    // use object streaming to receive the message
    private ObjectInputStream inp;
    private DataOutputStream out;

    public Updates(Socket clientSocket, int port ,Database fileManager){
        this.clientSocket = clientSocket;
        this.fileManager = fileManager;
        this.PORT = port;

        try {
            this.inp = new ObjectInputStream(this.clientSocket.getInputStream());
            this.out = new DataOutputStream(this.clientSocket.getOutputStream());
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
        //todo: stuff with the data
    }

}
