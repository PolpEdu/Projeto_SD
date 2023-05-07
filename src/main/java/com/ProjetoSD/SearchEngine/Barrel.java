package com.ProjetoSD.SearchEngine;

import com.ProjetoSD.SearchEngine.Database;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Semaphore;

/**
 * Classe que representa um barril de dados de um mecanismo de busca.
 */

public class Barrel extends Thread implements Serializable {
    /**
     * file to access the links
     */
    public final File linkfile;
    /**
     * file to access the words
     */
    public final File wordfile;
    /**
     * file to access the info
     */
    public final File infofile;
    /**
     * file to access the links - backup
     */

    public final File linkfileb;
    /**
     * file to access the words - backup
     */
    public final File wordfileb;
    /**
     * file to access the info - backup
     */
    public final File infofileb;
    /**
     * id of the barrel
     */
    private final int id; // id do barrel

    /**
     * multicast address to send the messages
     */
    private final String MULTICAST_ADDRESS;
    /**
     * multicast port
     */
    private final int MULTICAST_RECEIVE_PORT;
    /**
     * hashmap to associate words to links
     */

    private final HashMap<String, HashSet<String>> word_Links;
    /**
     * hashmap to associate link to links
     */
    private final HashMap<String, HashSet<String>> link_links;
    /**
     * hashmap to associate link to title and description
     */
    private final HashMap<String, ArrayList<String>> link_info;

    /**
     * semaphore to control the access to acknowledge
     */
    private final Semaphore ackSem;
    /**
     * size of the message
     */
    int messageSize = 8 * 1024;
    /**
     * database object to access the write and read methods
     */
    public Database files;
    /**
     * multicast group
     */
    private InetAddress group;
    /**
     * multicast socket
     */
    private MulticastSocket receiveSocket;

    /**
     * Construtor da classe Barrel.
     * @param id ID do barril.
     * @param MULTICAST_RECEIVE_PORT Porta multicast para downloaders.
     * @param MULTICAST_ADDRESS Endereço multicast para downloaders.
     * @param linkfile Arquivo de links.
     * @param wordfile Arquivo de palavras.
     * @param infofile Arquivo de informações.
     * @param files Instância do banco de dados.
     * @param ackSem Semáforo para controle de exclusão mútua.
     */

    public Barrel(int id, int MULTICAST_RECEIVE_PORT, String MULTICAST_ADDRESS, File linkfile, File wordfile, File infofile, Database files,  Semaphore ackSem) {
        this.id = id;
        this.receiveSocket = null;
        this.group = null;
        this.ackSem = ackSem;

        this.linkfile = linkfile;
        this.wordfile = wordfile;
        this.infofile = infofile;
        this.infofileb = new File("src/main/java/com/ProjetoSD/info-"+this.id+"backup");
        this.wordfileb = new File("src/main/java/com/ProjetoSD/words-"+this.id+"backup");
        this.linkfileb = new File("src/main/java/com/ProjetoSD/links-"+this.id+"backup");

        this.files = files;

        this.MULTICAST_ADDRESS = MULTICAST_ADDRESS;
        this.MULTICAST_RECEIVE_PORT = MULTICAST_RECEIVE_PORT;


        this.word_Links = files.getWords(wordfile, this.wordfileb);
        this.link_links = files.getLinks(linkfile, this.linkfileb);
        this.link_info = files.getLinksInfo(infofile, this.infofileb);

    }

    /**
     * Neste método recebemos várias mensagens multicast de downloaders, e as processamos.
     * Dependendo do tipo de mensagem, adicionamos numa lista geral ou atualizamos os arquivos.
     * Fazemos também acknowledgement para os downloaders.
     * @throws IOException
     */

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


            if (id.equals("done")) {
                String[] split;
                String splittype;
                String splitid;
                String downid = list[2];
                ArrayList<String> toRemove = new ArrayList<>();
                for (String str : queuelist) {

                    split = str.split("\\|");
                    splitid = split[0].split(":")[1];
                    splittype = split[1].split(":")[1];


                    if (splitid.equals("dwnl")) {
                        if (splittype.equals("word")) {
                            if (downid.equals(split[4])) {
                                if (!this.word_Links.containsKey(split[2])) {
                                    this.word_Links.put(split[2], new HashSet<>());
                                }
                                this.word_Links.get(split[2]).add(split[3]);
                                toRemove.add(str);
                            }

                        }
                        else if (splittype.equals("links")) {
                            if (downid.equals(split[4])) {
                                if (!this.link_links.containsKey(split[2])) {
                                    this.link_links.put(split[2], new HashSet<>());
                                }
                                this.link_links.get(split[2]).add(split[3]);

                                toRemove.add(str);
                            }
                        } else if (splittype.equals("siteinfo")) {

                            if (downid.equals(split[5])) {
                                if (!this.link_info.containsKey(split[2])) {
                                    this.link_info.put(split[2], new ArrayList<>());
                                }

                                this.link_info.get(split[2]).add(split[3]);
                                this.link_info.get(split[2]).add(split[4]);

                                toRemove.add(str);
                            }
                        }
                    }
                }
                for(String r: toRemove){
                    queuelist.remove(r);
                }

                // add to update buffer

                this.files.updateWords(word_Links, wordfile, this.wordfileb);
                this.files.updateLinks(link_links, linkfile, this.linkfileb);
                this.files.updateInfo(link_info, infofile, this.infofileb);
            }

            String send;

            if (id.equals("dwnl")) {
                queuelist.add(received);
                // System.out.println("[BARREL " + this.id + "] " + received);
                if (type.equals("word")) {

                    send = "id:ack|type:ack|" + list[4] + "|" + type;
                    this.sendMessage(send);

                    //System.out.println("test " + list[2] +" " +list[3]);
                } else if (type.equals("links")) {

                    send = "id:ack|type:ack|" + list[4] + "|" + type;
                    this.sendMessage(send);


                    //System.out.println("test " + list[2] + " " + list[3]);
                } else if (type.equals("siteinfo")) {

                    send = "id:ack|type:ack|" + list[5] + "|" + type;
                    this.sendMessage(send);

                }
            }
//            else{
//                System.out.println("[BARREL " + this.id + "] " + received);
//            }

        }
    }

    /**
     * Devolve os links associados a uma palavra que esta no hashmap
     * @param word palavra a procurar
     * @return links associados a uma palavra
     */
    public HashSet<String> getLinksAssciatedWord(String word) {
        return this.word_Links.get(word);
    }

    /**
     * Devolve a descrição de um link que esta no hashmap
     * @param link link a procurar
     * @return descrição do link
     */
    public String getLinkDescription(String link) {
        ArrayList<String> description = this.link_info.get(link);
        if (description == null) {
            return "No description.";
        }
        return this.link_info.get(link).get(1);
    }

    /**
     * devolve o titulo de um link que esta no hashmap
     * @param link link a procurar
     * @return titulo do link
     */
    public String getLinkTitle(String link) {
        ArrayList<String> title = this.link_info.get(link);
        if (title == null) {
            return "No title.";
        }
        return this.link_info.get(link).get(0);
    }

    /**
     * devolve links em que o link aparece , que esta no hashmap
     * @param link link a procurar
     * @return links em que o link aparece
     */
    public HashSet<String> getLinkPointers(String link){
        if (this.link_links.get(link) == null){
            return new HashSet<>();
        }
        return this.link_links.get(link);
    }

    /**
     * run do barrel para iniciar a socket multicast e a função loop
     */
    public void run() {
        System.out.println("[BARREL " + this.id + "] Barrel running...");

        try {
            // Multicast, receive from downloaders
            this.receiveSocket = new MulticastSocket(MULTICAST_RECEIVE_PORT);
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

    /**
     * envia uma mensagem multicast
     * @param send mensagem a enviar
     */
    private void sendMessage(String send) {
        try {
            this.ackSem.acquire();
            byte[] buffer = send.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, this.group, this.MULTICAST_RECEIVE_PORT);

            this.receiveSocket.send(packet);

            this.ackSem.release();

        } catch (InterruptedException | IOException e) {
            System.out.println("[EXCPETION] " + e.getMessage());
        }
    }


}
