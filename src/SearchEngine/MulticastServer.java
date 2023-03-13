package SearchEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Properties;

public class MulticastServer extends Thread {
    private String MULTICAST_ADDRESS = "224.3.2.1";
    private int PORT = 4321;
    private long SLEEP_TIME = 5000;

    public static void main(String[] args) {

        try {
            InputStream config = new FileInputStream("MulticastServer.properties");
            Properties prop = new Properties();
            prop.load(config);
            String tcp = prop.getProperty("HOST");
            System.out.println(tcp);


        } catch (IOException e) {

        }

        MulticastServer server = new MulticastServer();
        server.start();

        // MulticastClient client = new MulticastClient();
        // client.start();
    }

    public MulticastServer() {
        super("Server " + (long) (Math.random() * 1000));
    }

    public void run() {
        MulticastSocket socket = null;
        long counter = 0;
        System.out.println(this.getName() + " running...");
        try {
            socket = new MulticastSocket();  // create socket without binding it (only for sending)
            while (true) {
                String message = this.getName() + " packet " + counter++;
                byte[] buffer = message.getBytes();

                InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
                socket.send(packet);

                try { sleep((long) (Math.random() * SLEEP_TIME)); } catch (InterruptedException e) { }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }
}
