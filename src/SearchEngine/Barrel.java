package SearchEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.*;

public class Barrel extends Thread{
    private final String MULTICAST_ADDRESS;
    private int tcpPort;
    private String tcpHost;
    private InetAddress group;
    private MulticastSocket receiveSocket;// send socket do multicastserver
    private final int MULTICAST_RECEIVE_PORT;
    private final int id;

    private final HashMap<String, HashSet<String>> word_Links;
    private final HashMap<String, HashSet<String>> link_links;
    private final HashMap<String, ArrayList<String>> link_info;

    int messageSize = 8*1024;

    public Barrel(int id,int MULTICAST_RECEIVE_PORT, String MULTICAST_ADDRESS, String tcpHost, int tcpPort){
        this.MULTICAST_RECEIVE_PORT = MULTICAST_RECEIVE_PORT; // ouvir
        this.MULTICAST_ADDRESS = MULTICAST_ADDRESS;
        this.receiveSocket = null;
        this.group = null;
        this.tcpPort= tcpPort;
        this.tcpHost = tcpHost;
        this.id = id;
        this.word_Links = new HashMap<>();
        this.link_links = new HashMap<>();
        this.link_info = new HashMap<>();
    }



    public void run(){
        try {
            this.receiveSocket = new MulticastSocket(MULTICAST_RECEIVE_PORT);
            this.group = InetAddress.getByName(MULTICAST_ADDRESS);
            this.receiveSocket.joinGroup(this.group);

            while(true){
                byte[] receivebuffer = new byte[messageSize];
                DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
                this.receiveSocket.receive(receivePacket);

                String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
                String[] list = received.split("\\|");
                String id = list[0].split(":")[1];
                String type = list[1].split(":")[1];

                if(id.equals("dwnl")){
                    if(type.equals("word")){
                        if()
                        System.out.println("test " + list[2] +" " +list[3]);
                    }
                }



                //System.out.println("[BARREL " + (this.id+1) + "]" + received);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void main(String[] args) {
        try {
            InputStream input = new FileInputStream(new File("src/MulticastServer.properties").getAbsoluteFile());
            Properties MulticastServer = new Properties();
            MulticastServer.load(input);
            String tcpHost = MulticastServer.getProperty("HOST");
            int tcpPort = Integer.parseInt(MulticastServer.getProperty("PORT"));
            String multicastAddress = MulticastServer.getProperty("MC_ADDR");
            int receivePort = Integer.parseInt(MulticastServer.getProperty("MC_RECEIVE_PORT"));

            for (int i = 0; i < 1; i++) {
                Barrel barrel = new Barrel(i,receivePort, multicastAddress, tcpHost, tcpPort);
                barrel.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }







}
