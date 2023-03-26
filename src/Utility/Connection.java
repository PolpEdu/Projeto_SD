package Utility;

import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Semaphore;
import java.io.IOException;
import java.net.InetSocketAddress;

public class Connection extends Thread {
    private HashMap<String, HashSet<Integer>> onlinePorts;
    private Semaphore connectionSem;
    private int tcpPort;
    private String tcpHost;
    private CheckConnection checkConnection;

    public Connection(String tcpHost, int tcpPort, HashMap<String, HashSet<Integer>> onlinePorts, Semaphore connectionSem) {

        this.onlinePorts = onlinePorts;
        this.tcpHost = tcpHost;
        this.tcpPort = tcpPort;
        if (!this.onlinePorts.containsKey(tcpHost)) {
            this.onlinePorts.put(tcpHost, new HashSet<>());
        }
        this.onlinePorts.get(tcpHost).add(tcpPort);
        this.connectionSem = connectionSem;
        this.checkConnection = new CheckConnection(this.onlinePorts, this.connectionSem, this.tcpHost, this.tcpPort);
        this.start();
    }

    public HashMap<String, HashSet<Integer>> getPorts() {
        HashMap<String, HashSet<Integer>> o = null;
        try {
            this.connectionSem.acquire();
            o = this.onlinePorts;
            this.connectionSem.release();
        } catch (InterruptedException ei) {
        }
        return o;
    }

    public void run() {

        while (this.onlinePorts.size() > 0) {
            try {
                this.connectionSem.acquire();
                for (String address : this.onlinePorts.keySet()) {
                    for (int port : this.onlinePorts.get(address)) {
                        if (!address.equals(this.tcpHost) || port != this.tcpPort) {
                            // new TCPSender(port, address, this.tcpPort);
                        }
                    }
                }
                this.connectionSem.release();
                sleep(10000);
            } catch (InterruptedException e) {
                System.out.println("Sleep/Semaphore connection: " + e.getMessage());
            }
        }
    }

    public int getTcpPort() {
        return this.tcpPort;
    }

    public void updatePorts(String address, int port) {
        this.checkConnection.updatePorts(address, port);
    }
}

class CheckConnection extends Thread {
    private HashMap<String, HashSet<Integer>> onlinePorts;
    private Semaphore connectionSem;
    private String tcpHost;
    private int tcpPort;

    public CheckConnection(HashMap<String, HashSet<Integer>> onlinePorts, Semaphore connectionSem, String tcpHost, int tcpPort) {
        this.connectionSem = connectionSem;
        this.onlinePorts = onlinePorts;
        this.tcpHost = tcpHost;
        this.tcpPort = tcpPort;
        this.start();
    }

    public void run() {
        while (this.onlinePorts.size() > 0) {
            try {
                this.connectionSem.acquire();
                for (String address : this.onlinePorts.keySet()) {
                    for (int port : this.onlinePorts.get(address)) {
                        if ((!address.equals(this.tcpHost) || port != this.tcpPort) && !this.socketAlive(address, port)) {
                            this.onlinePorts.get(address).remove(port);
                        }
                    }
                    if (this.onlinePorts.get(address).size() == 0) {
                        this.onlinePorts.remove(address);
                    }
                }
                this.connectionSem.release();
                sleep(5000);
            } catch (InterruptedException e) {
                System.out.println("Semaphore connection: " + e.getMessage());
            }
        }
    }

    private boolean socketAlive(String host, int port) {
        int timeout = 3000;
        InetSocketAddress socketAddress = new InetSocketAddress(host, port);
        try {
            Socket socket = new Socket();
            socket.connect(socketAddress, timeout);
            socket.close();
            return true;
        } catch (IOException e) {
            System.out.println("Unable to connect to hostname: " + host + " port: " + port);
            return false;
        }
    }

    public void updatePorts(String address, int port) {
        try {
            this.connectionSem.acquire();
            if (!this.onlinePorts.containsKey(address)) {
                this.onlinePorts.put(address, new HashSet<>());
            }
            this.onlinePorts.get(address).add(port);
            this.connectionSem.release();
        } catch (InterruptedException e) {
            System.out.println("Semaphore connection: " + e.getMessage());
        }
    }
}