package SearchEngine;

import interfaces.RMIDownloaders;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Downloader extends Thread implements RMIDownloaders {

    private MulticastSocket receiveSocket;
    private final int MULTICAST_RECEIVE_PORT;
    private final String MULTICAST_ADDRESS;
    private InetAddress group;
    private final Semaphore conSem;
    private final  int rmiPort;
    private final String rmiHost;
    private final String rmiRegister;
    private RMIDownloaders queue;
    private int id;
    private ArrayBlockingQueue<String> urlQueue;


    public Downloader(int id,int MULTICAST_RECEIVE_PORT,String MULTICAST_ADDRESS, Semaphore conSem, int rmiPort, String rmiHost, String rmiRegister ) {
        this.receiveSocket = null;
        this.group  = null;
        this.conSem = conSem;
        this.MULTICAST_RECEIVE_PORT = MULTICAST_RECEIVE_PORT;
        this.MULTICAST_ADDRESS = MULTICAST_ADDRESS;
        this.rmiPort = rmiPort;
        this.rmiHost = rmiHost;
        this.rmiRegister = rmiRegister;
        this.id = id;
        queue = null;

    }

    public void run() {
        try {
            Registry r = LocateRegistry.createRegistry(rmiPort);
            System.setProperty("java.rmi.server.hostname", rmiHost);
            r.rebind(rmiRegister, this);

            this.receiveSocket = new MulticastSocket(MULTICAST_RECEIVE_PORT);
            this.group = InetAddress.getByName(MULTICAST_ADDRESS);
            this.receiveSocket.joinGroup(this.group);

            this.QueueInfo();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }



    }

    public static void main(String[] args) {
        System.getProperties().put("java.security.policy", "policy.all");

        try {
            Properties barrelProp = new Properties();
            barrelProp.load(new FileInputStream(new File("src/Barrel.properties").getAbsoluteFile()));

            Properties multicastServerProp = new Properties();
            multicastServerProp.load(new FileInputStream(new File("src/MulticastServer.properties").getAbsoluteFile()));


            String rmiHost = barrelProp.getProperty("HOST");
            String rmiRegister = barrelProp.getProperty("RMI_REGISTER");
            int rmiPort = Integer.parseInt(barrelProp.getProperty("PORT"));


            String multicastAddress = multicastServerProp.getProperty("MC_ADDR");
            int receivePort = Integer.parseInt(multicastServerProp.getProperty("MC_RECEIVE_PORT"));

            Semaphore listsem = new Semaphore(1);
            
            for (int i = 0; i < 1; i++) {

                if (rmiHost == null || rmiPort == 0 || multicastAddress == null || receivePort == 0) {
                    System.out.println("[DOWNLOADER" + i + "] Error reading properties file");
                    System.exit(1);
                }

                Downloader downloader = new Downloader(i, receivePort, multicastAddress, listsem, rmiPort, rmiHost, rmiRegister);
                downloader.start();
            }

        } catch (IOException e) {
            System.out.println("[BARREL] Error reading properties file:");
            e.printStackTrace();
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

                while (isempty()) {
                    sleep(500);
                }

                conSem.acquire();
                String link = this.takeLink();
                conSem.release();

                String message;
                ArrayList<String> links = new ArrayList<>();
                ArrayList<String> listWords = new ArrayList<>();
                ArrayList<String> info = new ArrayList<>();


                //linguagem regular de forma a nao receber caracteres especiais e apenas guardar numeros e letras
                Pattern pattern = Pattern.compile("^[a-zA-Z0-9]*$");
                if (getInfoFromWebsite(link, links, listWords, info)) {
                    for (String w : listWords) {
                        Matcher matcher = pattern.matcher(w);
                        if (matcher.matches()) {
                            message = "id:dwnl|type:word|" + w + "|" + link ;
                            this.sendMessage(message);
                        }

                    }
                    for (String l : links) {
                        message = "id:dwnl|type:links|" + l + "|" + link ;
                        this.sendMessage(message);
                    }

                    message = "id:dwnl|type:siteinfo|" + link + "|" + info.get(0) + "|" + info.get(1) ;

                    this.sendMessage(message);
                    //System.out.println(message);

                    //colocar os novos links na queue para continuar a ir buscar informação
                    for (String l : links) {
                        this.offerLink(l);
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Failed to check the queue and get the link");
                e.printStackTrace();

            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
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

    private void sendMessage(String send) {
        try {
            this.conSem.acquire();
            byte[] buffer = send.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, this.group, this.MULTICAST_RECEIVE_PORT);

            this.receiveSocket.send(packet);

            this.conSem.release();
        } catch (InterruptedException | IOException e) {
            System.out.println("[EXCPETION] " + e.getMessage());
        }
    }

    @Override
    public String takeLink() throws RemoteException {

        try {
            return this.urlQueue.take();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void offerLink(String link) throws RemoteException {
        this.urlQueue.offer(link);
    }

    @Override
    public boolean isempty() throws RemoteException {
        return this.urlQueue.isEmpty();
    }
}






