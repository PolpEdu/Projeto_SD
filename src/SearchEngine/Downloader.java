package SearchEngine;

import interfaces.RMIServerInterface;
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


public class Downloader extends Thread implements Remote {

    private final int MULTICAST_SEND_PORT;
    private final String MULTICAST_ADDRESS;
    private final Semaphore conSem;
    private MulticastSocket sendSocket;
    private InetAddress group;
    private final RMIServerInterface server;
    private final int id;

    public Downloader(int id, int MULTICAST_SEND_PORT, String MULTICAST_ADDRESS, Semaphore conSem,RMIServerInterface server) {
        this.sendSocket = null;
        this.group = null;
        this.conSem = conSem;

        this.MULTICAST_SEND_PORT = MULTICAST_SEND_PORT;

        this.MULTICAST_ADDRESS = MULTICAST_ADDRESS;

        this.id = id;
        this.server = server;

    }

    public static void main(String[] args) {
        System.getProperties().put("java.security.policy", "policy.all");

        try {
            Properties Prop = new Properties();
            Prop.load(new FileInputStream(new File("src/Downloader.properties").getAbsoluteFile()));

            Properties multicastServerProp = new Properties();
            multicastServerProp.load(new FileInputStream(new File("src/MulticastServer.properties").getAbsoluteFile()));

            String rmiHost = Prop.getProperty("RMI_HOST");
            String rmiRegister = Prop.getProperty("RMI_REGISTER");
            int rmiPort = Integer.parseInt(Prop.getProperty("RMI_PORT"));

            if (rmiHost == null || rmiPort == 0 || rmiRegister == null) {
                System.out.println("[DOWNLOADER] Error reading RMI config");
                System.out.println("[DOWNLOADER] Config: " + rmiHost + ":" + rmiPort + "/" + rmiRegister);
                return;
            }

            RMIServerInterface server = null;
            try {
                server = (RMIServerInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegister);

            } catch (RemoteException e) {
                System.out.println("[DOWNLOADER] Error connecting to RMI server:");
                System.out.println("[DOWNLOADER] Config: " + rmiHost + ":" + rmiPort + "/" + rmiRegister);
                e.printStackTrace();
                return;
            }

            String multicastAddress = multicastServerProp.getProperty("MC_ADDR");
            int sendPort = Integer.parseInt(multicastServerProp.getProperty("MC_RECEIVE_PORT"));//envia para os barrels


            Semaphore listsem = new Semaphore(1);

            for (int i = 1; i < 3; i++) {

                if (multicastAddress == null || sendPort == 0) {
                    System.out.println("[DOWNLOADER" + i + "] Error reading properties file");
                    System.exit(1);
                }

                Downloader downloader = new Downloader(i, sendPort, multicastAddress, listsem,server);
                downloader.start();
            }

        } catch (IOException e) {
            System.out.println("[BARREL] Error reading properties file:");
            e.printStackTrace();
        } catch (NotBoundException e) {
            throw new RuntimeException(e);
        }
    }

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
                String[] splited = line.split("[ ,;:.?!“”(){}\\[\\]<>'\n]+");
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
        return true;
    }

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
                            wo = "id:dwnl|type:word|" + w + "|" + link + "|" +  this.id;
                            boolean quit = false;
                            while(!quit) {
                                this.sendMessage(wo);
                                long tempoinicial = System.currentTimeMillis();
                                int messageSize = 8 * 1024;
                                byte[] receivebuffer = new byte[messageSize];
                                DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
                                while (System.currentTimeMillis() < (tempoinicial + 1000)) {
                                    this.sendSocket.receive(receivePacket);
                                    String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
                                    if(received.equals("id:ack|type:ack|"+this.id + "|" + "word") || System.currentTimeMillis() >= (tempoinicial + 1000)){
                                        quit = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    for (String l : links) {
                        li = "id:dwnl|type:links|" + l + "|" + link+ "|" + this.id;
                        this.sendMessage(li);
                        boolean quit = false;
                        while(!quit) {
                            this.sendMessage(li);
                            long tempoinicial = System.currentTimeMillis();

                            int messageSize = 8 * 1024;
                            byte[] receivebuffer = new byte[messageSize];
                            DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);

                            while (System.currentTimeMillis() < (tempoinicial + 1000)) {
                                this.sendSocket.receive(receivePacket);
                                String received = new String(receivePacket.getData(), 0, receivePacket.getLength());

                                if(received.equals("id:ack|type:ack|"+this.id + "|" + "links") || System.currentTimeMillis() >= (tempoinicial + 1000)){
                                    quit = true;
                                    break;
                                }
                            }

                        }
                    }

                    i = "id:dwnl|type:siteinfo|" + link + "|" + info.get(0) + "|" + info.get(1)+ "|" + this.id;
                    this.sendMessage(i);
                    boolean quit = false;
                    while(!quit) {
                        this.sendMessage(i);
                        long tempoinicial = System.currentTimeMillis();
                        int messageSize = 8 * 1024;
                        byte[] receivebuffer = new byte[messageSize];
                        DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);

                        while (System.currentTimeMillis() < (tempoinicial + 1000)) {
                            this.sendSocket.receive(receivePacket);
                            String received = new String(receivePacket.getData(), 0, receivePacket.getLength());

                            if(received.equals("id:ack|type:ack|"+this.id + "|" + "siteinfo") || System.currentTimeMillis() >= (tempoinicial + 1000)){
                                quit = true;
                                break;
                            }
                        }


                    }

                    System.out.println("[DOWNLOADER " + this.id + "] ALL INFO SENDED");
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






