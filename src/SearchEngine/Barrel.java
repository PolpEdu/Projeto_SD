package SearchEngine;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Barrel extends Thread{
    public int MULTICAST_SEND_PORT;
    private String MULTICAST_ADDRESS;
    private String tcpHost;
    MulticastSocket receiveSocket;// send socket do multicastserver
    InetAddress group;
    private int tcpPort;

    int messageSize = 8*1024;

    public Barrel(int MULTICAST_SEND_PORT, String MULTICAST_ADDRESS, String tcpHost, int tcpPort, MulticastSocket receiveSocket, InetAddress group ){
        this.MULTICAST_SEND_PORT = MULTICAST_SEND_PORT;
        this.MULTICAST_ADDRESS = MULTICAST_ADDRESS;
        this.tcpHost = tcpHost;
        this.receiveSocket = receiveSocket;
        this.group = group;
        this.tcpPort = tcpPort;
        this.start();
    }

    public void run(){this.listenPort();}

    public void listenPort(){
        try {
            byte[] receivebuffer = new byte[messageSize];
            DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
            this.receiveSocket.receive(receivePacket);
            String received = new String(receivePacket.getData(), 0, receivePacket.getLength());

            System.out.println(received);
        } catch (IOException e) {
            System.out.println("[EXCEPTION] " + e.getMessage());
            e.printStackTrace();

        }
    }



}
