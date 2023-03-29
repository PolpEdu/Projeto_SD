package Utility;

import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

public class MulticastReceive extends Thread {
    // size of the message in bytes
    int messageSize = 1024 * 8;
    // socket Timeout
    int socketTimeout = 1500;
    // this is the group that will receive the messages
    InetAddress group;
    MulticastSend multicastSend;
    MulticastSocket socket;
    Semaphore sem;
    // this is the multicast that will receive the messages
    private String MULTICAST_ADDRESS;
    // this is the port that will receive the messages
    private int PORT;


    public MulticastReceive(MulticastSend multicastSend, String multicastAddress, int receivePort) {
        this.sem = new Semaphore(1);
        this.multicastSend = multicastSend;
        this.MULTICAST_ADDRESS = multicastAddress;
        this.PORT = receivePort;

        try {
            this.socket = new MulticastSocket(this.PORT);
            this.group = InetAddress.getByName(this.MULTICAST_ADDRESS);
        } catch (UnknownHostException eu) {
            System.out.println("[EXCEPTION] UnknownHostException");
            eu.printStackTrace();
        } catch (IOException ei) {
            System.out.println("[EXCEPTION] IOException");
            ei.printStackTrace();
        }
    }

    public void run() {
        try {
            // wait for the socket to be ready
            this.socket.setSoTimeout(socketTimeout);
            this.socket.joinGroup(this.group);
            while (true) {
            }
        } catch (SocketException e) {
            System.out.println("[EXCEPTION] SocketException");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("[EXCEPTION] IOException");
            e.printStackTrace();
        }
        socket.close();

    }

    public String parseRecievedPacket(Message msg, LinkedList<Message> sendQueue) {
        String message = null;
        boolean checkInQueue;
        System.out.println("[RECEIVED] " + msg.message);
        while (message == null) {
            byte[] buffer = new byte[messageSize];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                // print socket info
                this.socket.receive(packet);
                message = new String(packet.getData(), 0, packet.getLength());
                String[] messageArray = message.split("\\|");
                String id = messageArray[1].split(":")[1];
                if (msg.id.equals(id)) {
                    System.out.println("[RECEIVED] Received our first ACK: " + message);
                } else {
                    checkInQueue = false;
                    for (Message m : sendQueue) {
                        if (m.id.equals(id)) {
                            checkInQueue = true; // message is in queue
                            break;
                        }
                    }
                    if (!checkInQueue) {
                        // this happens when the server sends a message to the client and the client sends an ACK to the server
                        System.out.println("[RECEIVED] Received another ACK: " + message);
                        Message msgACK = new Message(id, "type:ack");
                        this.multicastSend.sendInfo(msgACK, sendQueue); // send again
                    } else {
                        buffer = new byte[messageSize];
                        packet = new DatagramPacket(buffer, buffer.length, this.group, this.PORT);
                        this.socket.send(packet);
                    }
                    message = null;
                }
            } catch (SocketTimeoutException e) {
                message = null;
                System.out.println("[EXCEPTION] Couldn't connect to Multicast Server. SocketTimeoutException: " + e.getMessage());
                e.printStackTrace();
                this.multicastSend.sendInfo(msg, sendQueue); // send again
            } catch (IOException e) {
                System.out.println("[EXCEPTION] IOException: " + e.getMessage());
                e.printStackTrace();
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("[EXCEPTION] ArrayIndexOutOfBoundsException: " + e.getMessage());
                System.out.println("[EXCEPTION] Message: " + message);
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("[EXCEPTION] Exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return message;
    }
}
