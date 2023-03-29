package SearchEngine;

import Client.User;
import interfaces.RMIBarrelInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Semaphore;

class Barrel extends Thread implements Serializable {
    static final int alive_checks = 5;
    static final int await_time = 2000;

    private final int id; // id do barrel

    // multicast from downloaders
    private final String MULTICAST_ADDRESS;
    private final int MULTICAST_RECEIVE_PORT;

    private final int MULTICAST_SEND_PORT;

    private final HashMap<String, HashSet<String>> word_Links;
    private final HashMap<String, HashSet<String>> link_links;
    private final HashMap<String, ArrayList<String>> link_info;
    private final HashMap<String, HashSet<String>> users;


    private final int rmiPort;
    private final String rmiHost;
    private final String rmiRegister;

    private final File linkfile;
    private final File wordfile;
    private final File infofile;

    private final File usersfile;

    int messageSize = 8 * 1024;
    Database files;

    private final Semaphore ackSem;

    private InetAddress group;
    private RMIBarrelInterface b;
    private MulticastSocket receiveSocket;
    private MulticastSocket sendSocket;



    public Barrel(int id, int MULTICAST_RECEIVE_PORT, String MULTICAST_ADDRESS, String rmiHost, int rmiPort, String rmiRegister, File linkfile, File wordfile, File infofile, File usersfile, RMIBarrelInterface barrelInterface, Database files, int MULTICAST_SEND_PORT, Semaphore ackSem) {
        this.id = id;
        this.receiveSocket = null;
        this.sendSocket = null;
        this.group = null;
        this.ackSem = ackSem;

        this.linkfile = linkfile;
        this.wordfile = wordfile;
        this.infofile = infofile;
        this.usersfile = usersfile;


        this.files = files;

        this.MULTICAST_ADDRESS = MULTICAST_ADDRESS;
        this.MULTICAST_RECEIVE_PORT = MULTICAST_RECEIVE_PORT;
        this.MULTICAST_SEND_PORT = MULTICAST_SEND_PORT;

        this.rmiPort = rmiPort;
        this.rmiHost = rmiHost;
        this.rmiRegister = rmiRegister;
        this.b = barrelInterface;

        this.word_Links = files.getWords(wordfile);
        this.link_links = files.getLinks(linkfile);
        this.link_info = files.getLinksInfo(infofile);

        this.users = new HashMap<>();

    }

    public void loop() throws IOException {
        ArrayList<String> queuelist = new ArrayList<>();
        while (true) {

            byte[] receivebuffer = new byte[messageSize];
            DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
            this.receiveSocket.receive(receivePacket);


            String received = new String(receivePacket.getData(), 0, receivePacket.getLength());


            String[] list = received.split("\\|");
            String id = list[0].split(":")[1];
            String type = list[1].split(":")[1];


           if(id.equals("done")){
                String[] split;
                String splittype;
                String splitid;

                for(String str: queuelist){
                    
                   split =  str.split("\\|");
                   splitid = split[0].split(":")[1];
                   splittype = split[1].split(":")[1];
                    if (splitid.equals("dwnl")) {
                        // System.out.println("[BARREL " + this.id + "] " + received);
                        if (splittype.equals("word")) {
                            if(id.equals(split[4])){
                                if (!this.word_Links.containsKey(split[2])) {
                                    this.word_Links.put(split[2], new HashSet<>());
                                }
                                this.word_Links.get(split[2]).add(split[3]);
                                this.files.updateWords(word_Links, wordfile);
                                queuelist.remove(str);
                            }

                            //System.out.println("test " + list[2] +" " +list[3]);
                        } else if (splittype.equals("links")) {
                            if(id.equals(split[4])) {
                                if (!this.link_links.containsKey(split[2])) {
                                    this.link_links.put(split[2], new HashSet<>());
                                }
                                this.link_links.get(split[2]).add(split[3]);
                                this.files.updateLinks(link_links, linkfile);
                                queuelist.remove(str);
                            }
                            //System.out.println("test " + list[2] + " " + list[3]);
                        } else if (splittype.equals("siteinfo")) {
                            if(id.equals(split[5])) {
                                if (!this.link_info.containsKey(split[2])) {
                                    this.link_info.put(split[2], new ArrayList<>());
                                }

                                this.link_info.get(split[2]).add(split[3]);
                                this.link_info.get(split[2]).add(split[4]);

                                this.files.updateInfo(link_info, infofile);
                                queuelist.remove(str);
                            }
                        }
                    }

                }
            }

            String send;


            if (id.equals("dwnl")) {
                queuelist.add(received);
                // System.out.println("[BARREL " + this.id + "] " + received);
                if (type.equals("word")) {

                    send = "id:ack|type:ack|"+list[4] + "|" + type;
                    this.sendMessage(send);

                    //System.out.println("test " + list[2] +" " +list[3]);
                } else if (type.equals("links")) {

                    send = "id:ack|type:ack|"+list[4] + "|" + type;
                    this.sendMessage(send);


                    //System.out.println("test " + list[2] + " " + list[3]);
                } else if (type.equals("siteinfo")) {

                    send = "id:ack|type:ack|"+list[5] + "|" + type;
                    this.sendMessage(send);

                }
            }
//            else{
//                System.out.println("[BARREL " + this.id + "] " + received);
//            }

        }
    }

    public void run() {
        System.out.println("[BARREL " + this.id + "] Barrel running...");

        try {
            // Multicast, receive from downloaders
            this.receiveSocket = new MulticastSocket(MULTICAST_RECEIVE_PORT);
            this.sendSocket = new MulticastSocket(MULTICAST_SEND_PORT);
            this.group = InetAddress.getByName(MULTICAST_ADDRESS);
            this.receiveSocket.joinGroup(this.group);

            loop();
        } catch (UnknownHostException e) {
            System.out.println("[EXCEPTION] UnknownHostException");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendMessage(String send) {
        try {
            this.ackSem.acquire();
            byte[] buffer = send.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, this.group, this.MULTICAST_RECEIVE_PORT);

            this.sendSocket.send(packet);

            this.ackSem.release();

        } catch (InterruptedException | IOException e) {
            System.out.println("[EXCPETION] " + e.getMessage());
        }
    }


}
