package Utility;

import Utility.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.LinkedList;

public class MulticastSend {
    MulticastSocket socket;
    InetAddress group;
    private String MULTICAST_ADDRESS;
    private int PORT;

    public MulticastSend(String multicastAddress, int sendPort) {
        this.PORT = sendPort;
        this.MULTICAST_ADDRESS = multicastAddress;

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

    public void sendInfo(Message m, LinkedList<Message> sendQueue) {
        // check if message type is ack
        String type = m.getType();
        byte[] buffer = m.message.getBytes();

        if (!type.equals("ack")) {
            sendQueue.offer(m);
        }

        try {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, this.group, this.PORT);
            this.socket.send(packet);
        } catch (IOException e) {
            System.out.println("[EXCEPTION] IOException");
            e.printStackTrace();
        }
    }
}