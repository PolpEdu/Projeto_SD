package com.ProjetoSD.SearchEngine;

import com.ProjetoSD.interfaces.RMIUrlQueueInterface;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Downloader class
 * this class is responsible for downloading the pages from the queue
 */
class Downloader extends Thread implements Remote {
    /**
     * multicast send port
     */
    private final int MULTICAST_SEND_PORT;
    /**
     * multicast address
     */
    private final String MULTICAST_ADDRESS;
    /**
     * semaphore to control the access to the send messages
     */
    private final Semaphore conSem;
    /**
     * RMI server to the urlqueue
     */
    private final RMIUrlQueueInterface server;
    /**
     * id of the downloader
     */
    private final int id;
    /**
     * socket to send the multicast
     */
    private MulticastSocket sendSocket;
    /**
     * group to send the multicast
     */
    private InetAddress group;


    /**
     * constructor of the Downloader class
     *
     * @param id                  - id of the downloader
     * @param MULTICAST_SEND_PORT - port to send the multicast
     * @param MULTICAST_ADDRESS   - address to send the multicast
     * @param conSem              - semaphore to control the access to the queue
     * @param server              - RMI server
     */
    public Downloader(int id, int MULTICAST_SEND_PORT, String MULTICAST_ADDRESS, Semaphore conSem, RMIUrlQueueInterface server) {
        this.sendSocket = null;
        this.group = null;
        this.conSem = conSem;

        this.MULTICAST_SEND_PORT = MULTICAST_SEND_PORT;

        this.MULTICAST_ADDRESS = MULTICAST_ADDRESS;

        this.id = id;
        this.server = server;
    }

    /**
     * main method of the Downloader class
     * here we make the connection to the multicast and the RMI server and start the download
     * @param args
     */
    public static void main(String[] args) {
        System.getProperties().put("java.security.policy", "policy.all");
        InputStream in = System.in;
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        try {
            Properties Prop = new Properties();
            Prop.load(new FileInputStream(new File("src/main/java/com/ProjetoSD/Downloader.properties").getAbsoluteFile()));

            Properties multicastServerProp = new Properties();
            multicastServerProp.load(new FileInputStream(new File("src/main/java/com/ProjetoSD/MulticastServer.properties").getAbsoluteFile()));

            String rmiHost = Prop.getProperty("RMI_HOST");
            String rmiRegister = Prop.getProperty("RMI_REGISTER");
            int rmiPort = Integer.parseInt(Prop.getProperty("RMI_PORT"));
            System.out.println("GIVE ID: ");
            int id = Integer.parseInt(br.readLine());
            if (rmiHost == null || rmiPort == 0 || rmiRegister == null) {
                System.out.println("[DOWNLOADER] Error reading RMI config");
                System.out.println("[DOWNLOADER] Config: " + rmiHost + ":" + rmiPort + "/" + rmiRegister);
                return;
            }

            RMIUrlQueueInterface server;
            try {
                server = (RMIUrlQueueInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegister);

            } catch (RemoteException e) {
                System.out.println("[DOWNLOADER] Error connecting to RMI server:");
                System.out.println("[DOWNLOADER] Config: " + rmiHost + ":" + rmiPort + "/" + rmiRegister);
                e.printStackTrace();
                return;
            }

            String multicastAddress = multicastServerProp.getProperty("MC_ADDR");
            int sendPort = Integer.parseInt(multicastServerProp.getProperty("MC_RECEIVE_PORT"));//envia para os barrels


            Semaphore listsem = new Semaphore(1);

            for (int i = 1; i < 2; i++) {

                if (multicastAddress == null || sendPort == 0) {
                    System.out.println("[DOWNLOADER" + id + "] Error reading properties file");
                    System.exit(1);
                }

                Downloader downloader = new Downloader(id, sendPort, multicastAddress, listsem, server);
                downloader.start();
            }

        } catch (IOException e) {
            System.out.println("[BARREL] Error reading properties file:");
            e.printStackTrace();
        } catch (NotBoundException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     *
     * In this function we get an array and a list to add words
     * We process the words with a stop word list and add them to the list
     *
     * @param words array with words to be processed
     * @param wordList list to add the words
     */
    private static void seperateWords(String words, ArrayList<String> wordList) {
        BufferedReader buffer = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(words.getBytes(StandardCharsets.UTF_8))));
        String line;
        String[] pois = {"de", "sobre", "a", "o", "que", "e", "do", "da", "em", "um", "para", "é", "com", "não", "uma", "os", "no", "se", "na", "por", "mais", "as", "dos", "como", "mas", "foi", "ao", "ele", "das", "tem", "à", "seu", "sua", "ou", "ser", "quando", "muito", "há", "nos", "já", "está", "eu", "também", "só", "pelo", "pela", "até", "isso", "ela", "entre", "era", "depois", "sem", "mesmo", "aos", "ter", "seus", "quem", "nas", "me", "esse", "eles", "estão", "você", "tinha", "foram", "essa", "num", "nem", "suas", "meu", "às", "minha", "têm", "numa", "pelos", "elas", "havia", "seja", "qual", "será", "nós", "tenho", "lhe", "deles", "essas", "esses", "pelas", "este", "fosse", "dele", "tu", "te", "vocês", "vos", "lhes", "meus", "minhas", "teu", "tua", "teus", "tuas", "nosso", "nossa", "nossos", "nossas", "dela", "delas", "esta", "estes", "estas", "aquele", "aquela", "aqueles", "aquelas", "isto", "aquilo", "estou", "está", "estamos", "estão", "estive", "esteve", "estivemos", "estiveram", "estava", "estávamos", "estavam", "estivera", "estivéramos", "esteja", "estejamos", "estejam", "estivesse", "estivéssemos", "estivessem", "estiver", "estivermos", "estiverem", "hei", "há", "havemos", "hão", "houve", "houvemos", "houveram", "houvera", "houvéramos", "haja", "hajamos", "hajam", "houvesse", "houvéssemos", "houvessem", "houver", "houvermos", "houverem", "houverei", "houverá", "houveremos", "houverão", "houveria", "houveríamos", "houveriam", "sou", "somos", "são", "era", "éramos", "eram", "fui", "foi", "fomos", "foram", "fora", "fôramos", "seja", "sejamos", "sejam", "fosse", "fôssemos", "fossem", "for", "formos", "forem", "serei", "será", "seremos", "serão", "seria", "seríamos", "seriam", "tenho", "tem", "temos", "tém", "tinha", "tínhamos", "tinham", "tive", "teve", "tivemos", "tiveram", "tivera", "tivéramos", "tenha", "tenhamos", "tenham", "tivesse", "tivéssemos", "tivessem", "tiver", "tivermos", "tiverem", "terei", "terá", "teremos", "terão", "teria", "teríamos", "teriam"};
        ArrayList<String> stopWords = new ArrayList<>(Arrays.asList(pois));

        while (true) {

            try {
                line = buffer.readLine();
                if (line == null) {
                    break;
                }
                String[] splited = line.split("[ ,;:.?“”!(){}\\[\\]<>'\n]+");
                for (String word : splited) {
                    word = word.toLowerCase();
                    if (!wordList.contains(word) && !"".equals(word) && !stopWords.contains(word)) {
                        wordList.add(word);
                    }
                }
            } catch (IOException e) {
                System.out.println("[EXCPETION] " + e.getMessage());
                e.printStackTrace();
            }
        }

        try {
            buffer.close();
        } catch (IOException e) {
            System.out.println("[EXCPETION] " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * run function to start the thread and the socket to send messages to the downloaders
     */
    public void run() {
        System.out.println("[DOWNLOADER " + this.id + "] is running ...");
        try {
            this.sendSocket = new MulticastSocket(MULTICAST_SEND_PORT);
            this.group = InetAddress.getByName(MULTICAST_ADDRESS);
            this.sendSocket.joinGroup(this.group);

            this.QueueInfo();
        } catch (IOException e) {
            System.out.println("DA LHE PUTO");
        }
    }

    /**
     * function to crawl in the website and get the info from it
     * add to the arrays links, words and title and description
     *
     *
     * @param webs - website to get info
     * @param Links - links from website
     * @param wordL - words from website
     * @param SiteInfo - title and description from website
     * @return true if website is valid, false if not
     */
    boolean getInfoFromWebsite(String webs, ArrayList<String> Links, ArrayList<String> wordL, ArrayList<String> SiteInfo) {
        String ws = webs;
        try {
            if (!ws.startsWith("http://") && !ws.startsWith("https://")) {
                ws = "http://".concat(ws);
            }

            Document doc = Jsoup.connect(ws).get();

            String title = doc.title();

            String desciption = doc.select("meta[name=description]").attr("content");
            if (desciption.equals("")) {
                desciption = "This page has no description";
            }
            SiteInfo.add(title);
            SiteInfo.add(desciption);

            Elements hrefs = doc.select("a[href]");
            // System.out.println("Links: " + hrefs.size());
            for (Element link : hrefs) {
                if (!link.attr("href").startsWith("#") || link.attr("href").startsWith("http")) {
                    Links.add(link.attr("href"));
                }
            }

            String words = doc.text();
            seperateWords(words, wordL);

        } catch (org.jsoup.HttpStatusException e) {
            System.out.println("[EXCEPTION] Getting info from website: " + ws);
            // e.printStackTrace();
            return false;
        } catch (IOException e) {
            System.out.println("[EXCEPTION] Getting info from website: " + ws);
            // e.printStackTrace();
            return false;
        }
        catch (IllegalArgumentException e){
            System.out.println("[EXCEPTION] Getting info from website: " + ws);
            return false;
        }
        return true;
    }


    /**
     * function to send the info to the barrels
     * in this function we get a link from the urlqueue
     * and then we get the info from the website of that link
     * we process all the info and send to the barrels and wait for the ack
     * if we dont get the ack we send the message again
     * in the and we send the link to the barrel to say that we already processed all that link
     */
    void QueueInfo() {

        while (true) {
            try {

                String link = server.takeLink();

                if (link == null) {
                    while (server.isempty()) {
                        sleep(1000);
                    }
                }
                String li;
                String wo;
                String i;
                ArrayList<String> links = new ArrayList<>();
                ArrayList<String> listWords = new ArrayList<>();
                ArrayList<String> info = new ArrayList<>();

                //linguagem regular de forma a nao receber caracteres especiais e apenas guardar numeros e letras
                Pattern pattern = Pattern.compile("^[a-zA-Z0-9]*$");
                if (getInfoFromWebsite(link, links, listWords, info)) {
                    for (String w : listWords) {
                        Matcher matcher = pattern.matcher(w);
                        if (matcher.matches()) {
                            wo = "id:dwnl|type:word|" + w + "|" + link + "|" + this.id;
                            boolean quit = false;
                            while (!quit) {
                                this.sendMessage(wo);
                                long tempoinicial = System.currentTimeMillis();
                                int messageSize = 8 * 1024;
                                byte[] receivebuffer = new byte[messageSize];
                                DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
                                while (System.currentTimeMillis() < (tempoinicial + 1000)) {
                                    this.sendSocket.receive(receivePacket);
                                    String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
                                    if (received.equals("id:ack|type:ack|" + this.id + "|" + "word") || System.currentTimeMillis() >= (tempoinicial + 1000)) {
                                        quit = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    for (String l : links) {
                        li = "id:dwnl|type:links|" + l + "|" + link + "|" + this.id;
                        this.sendMessage(li);
                        boolean quit = false;
                        while (!quit) {
                            this.sendMessage(li);
                            long tempoinicial = System.currentTimeMillis();

                            int messageSize = 8 * 1024;
                            byte[] receivebuffer = new byte[messageSize];
                            DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);

                            while (System.currentTimeMillis() < (tempoinicial + 1000)) {
                                this.sendSocket.receive(receivePacket);
                                String received = new String(receivePacket.getData(), 0, receivePacket.getLength());

                                if (received.equals("id:ack|type:ack|" + this.id + "|" + "links") || System.currentTimeMillis() >= (tempoinicial + 1000)) {
                                    quit = true;
                                    break;
                                }
                            }
                        }
                    }

                    i = "id:dwnl|type:siteinfo|" + link + "|" + info.get(0) + "|" + info.get(1) + "|" + this.id;
                    this.sendMessage(i);
                    boolean quit = false;
                    while (!quit) {
                        this.sendMessage(i);
                        long tempoinicial = System.currentTimeMillis();
                        int messageSize = 8 * 1024;
                        byte[] receivebuffer = new byte[messageSize];
                        DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);

                        while (System.currentTimeMillis() < (tempoinicial + 1000)) {
                            this.sendSocket.receive(receivePacket);
                            String received = new String(receivePacket.getData(), 0, receivePacket.getLength());

                            if (received.equals("id:ack|type:ack|" + this.id + "|" + "siteinfo") || System.currentTimeMillis() >= (tempoinicial + 1000)) {
                                quit = true;
                                break;
                            }
                        }


                    }

                    System.out.println("[DOWNLOADER " + this.id + "] ALL INFO SENDED");

                    // print the first info
                    // System.out.println("[DOWNLOADER " + this.id + "] " + info.get(0));

                    sendMessage("id:done|type:ack|" + this.id);
                    //colocar os novos links na queue para continuar a ir buscar informaçã
                    for (String l : links) {
                        server.offerLink(l);
                    }
                }
            } catch (RemoteException e) {
                // todo: stay in a loop and try to reconnect
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * function to send a message to the multicast group
     *
     * @param send string to send
     */
    private void sendMessage(String send) {
        try {
            this.conSem.acquire();
            byte[] buffer = send.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, this.group, this.MULTICAST_SEND_PORT);

            this.sendSocket.send(packet);

            this.conSem.release();

        } catch (InterruptedException | IOException e) {
            System.out.println("[EXCPETION] " + e.getMessage());
        }
    }


}






