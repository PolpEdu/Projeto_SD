package SearchEngine;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.*;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Downloader extends Thread {

    private LinkedBlockingQueue<String> urlQueue;
    private MulticastSocket receiveSocket;
    private int MULTICAST_RECEIVE_PORT;
    private InetAddress group;
    private HashMap<String, HashSet<Integer>> onlinePorts;

    private Semaphore conSem;
    private int tcpPort;
    private String tcpHost;

    public Downloader(UrlQueue urlQueue,MulticastSocket receiveSocket, InetAddress group, HashMap<String, HashSet<Integer>> onlinePorts, Semaphore conSem, int tcpPort, String tcpHost){

        this.urlQueue = urlQueue.getUrlQueue();
        this.receiveSocket = receiveSocket;
        this.group = group;
        this.onlinePorts = onlinePorts;
        this.conSem = conSem;
        this.tcpPort = tcpPort;
        this. tcpHost = tcpHost;
        this.start();
    }

    public void run() {
        this.QueueInfo();
    }

    boolean getInfoFromWebsite(String webs, ArrayList<String> Links, ArrayList<String> Words, ArrayList<String> SiteInfo) {

        try {
            String ws = webs;
            if(!ws.startsWith("http://") && !ws.startsWith("https://")){
                ws = "http://".concat(ws);
            }


            Document doc = Jsoup.connect(ws).get();

            String title = doc.title();

            String desciption = doc.select("meta[name=description]").attr("content");
            if(desciption.equals("")){
                desciption = "This page has no description";
            }
            SiteInfo.add(title);
            SiteInfo.add(desciption);

            Elements hrefs = doc.select("a[href]");
            for(Element link: hrefs){
                if(!link.attr("href").startsWith("#") || link.attr("href").startsWith("http")){
                    Links.add(link.attr("href"));
                }
            }

            String words = doc.text();
            seperateWords(words, Words);

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    void QueueInfo(){

        while(true){
            try{

                while(urlQueue.isEmpty()){
                    sleep(1000);
                }
                String link = this.urlQueue.take();

                StringBuilder message = new StringBuilder();
                ArrayList<String> links = new ArrayList<>();
                ArrayList<String> listWords = new ArrayList<>();
                ArrayList<String> info = new ArrayList<>();


                //linguagem regular de forma a nao receber caracteres especiais e apenas guardar numeros e letras
                Pattern pattern = Pattern.compile("^[a-zA-Z0-9]*$");
                if(getInfoFromWebsite(link, links, listWords, info)){
                    for(String w: listWords){
                        Matcher matcher = pattern.matcher(w);
                        if(matcher.matches()){
                            message.append("word|");
                            message.append(w);
                            message.append("|");
                            message.append(link);
                            message.append(";");
                        }

                    }
                    for(String l: links){
                        message.append("link|");
                        message.append(l);
                        message.append("|");
                        message.append(link);
                        message.append(";");
                    }

                    message.append("siteinfo|");
                    message.append(info.get(0));
                    message.append("|");
                    message.append(info.get(1));
                    message.append(";");

                    System.out.println(message);

                    //colocar os novos links na queue para continuar a ir buscar informação
                    for (String l: links){
                        this.addUrl(l);
                    }
                }

            }
            catch(InterruptedException e){
                System.out.println("Failed to check the queue and get the link");
            }
        }
    }

    void addUrl(String l){
        this.urlQueue.offer(l);
    }

    private static void seperateWords(String words, ArrayList<String> Words){
        BufferedReader buffer = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(words.getBytes(StandardCharsets.UTF_8))));
        String line;
        String[] pois = {"de", "sobre", "a",  "o",  "que",  "e",  "do",  "da",  "em",  "um",  "para",  "é",  "com",  "não",  "uma",  "os",  "no",  "se",  "na",  "por",  "mais",  "as",  "dos",  "como",  "mas",  "foi",  "ao",  "ele",  "das",  "tem",  "à",  "seu",  "sua",  "ou",  "ser",  "quando",  "muito",  "há",  "nos",  "já",  "está",  "eu",  "também",  "só",  "pelo",  "pela",  "até",  "isso",  "ela",  "entre",  "era",  "depois",  "sem",  "mesmo",  "aos",  "ter",  "seus",  "quem",  "nas",  "me",  "esse",  "eles",  "estão",  "você",  "tinha",  "foram",  "essa",  "num",  "nem",  "suas",  "meu",  "às",  "minha",  "têm",  "numa",  "pelos",  "elas",  "havia",  "seja",  "qual",  "será",  "nós",  "tenho",  "lhe",  "deles",  "essas",  "esses",  "pelas",  "este",  "fosse",  "dele",  "tu",  "te",  "vocês",  "vos",  "lhes",  "meus",  "minhas",  "teu",  "tua",  "teus",  "tuas",  "nosso",  "nossa",  "nossos",  "nossas",  "dela",  "delas",  "esta",  "estes",  "estas",  "aquele",  "aquela",  "aqueles",  "aquelas",  "isto",  "aquilo",  "estou",  "está",  "estamos",  "estão",  "estive",  "esteve",  "estivemos",  "estiveram",  "estava",  "estávamos",  "estavam",  "estivera",  "estivéramos",  "esteja",  "estejamos",  "estejam",  "estivesse",  "estivéssemos",  "estivessem",  "estiver",  "estivermos",  "estiverem",  "hei",  "há",  "havemos",  "hão",  "houve",  "houvemos",  "houveram",  "houvera",  "houvéramos",  "haja",  "hajamos",  "hajam",  "houvesse",  "houvéssemos",  "houvessem",  "houver",  "houvermos",  "houverem",  "houverei",  "houverá",  "houveremos",  "houverão",  "houveria",  "houveríamos",  "houveriam",  "sou",  "somos",  "são",  "era",  "éramos",  "eram",  "fui",  "foi",  "fomos",  "foram",  "fora",  "fôramos",  "seja",  "sejamos",  "sejam",  "fosse",  "fôssemos",  "fossem",  "for",  "formos",  "forem",  "serei",  "será",  "seremos",  "serão",  "seria",  "seríamos",  "seriam",  "tenho",  "tem",  "temos",  "tém",  "tinha",  "tínhamos",  "tinham",  "tive",  "teve",  "tivemos",  "tiveram",  "tivera",  "tivéramos",  "tenha",  "tenhamos",  "tenham",  "tivesse",  "tivéssemos",  "tivessem",  "tiver",  "tivermos",  "tiverem",  "terei",  "terá",  "teremos",  "terão",  "teria",  "teríamos",  "teriam"};
        ArrayList<String> stopWords = new ArrayList<>(Arrays.asList(pois));

        while(true){

            try{
                line = buffer.readLine();
                if(line == null){
                    break;
                }
                String[] splited = line.split("[ ,;:.?!“”(){}\\[\\]<>'\n]+");
                for(String word: splited){
                    word = word.toLowerCase();
                    if(!Words.contains(word) && !"".equals(word) && !stopWords.contains(word)){
                        Words.add(word);
                    }
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

        try{
            buffer.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }


}






