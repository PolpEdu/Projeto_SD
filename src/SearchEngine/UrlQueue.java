package SearchEngine;

import java.util.concurrent.LinkedBlockingQueue;

public class UrlQueue extends Thread{
    private static LinkedBlockingQueue<String> urlQueue;

    public UrlQueue() {
        urlQueue = new LinkedBlockingQueue<>();
        urlQueue.offer("https://www.uc.pt/");
    }

    public static LinkedBlockingQueue<String> getUrlQueue(){
        return urlQueue;
    }
}
