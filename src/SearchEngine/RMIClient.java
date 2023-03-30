package SearchEngine;

import Client.Client;
import interfaces.RMIServerInterface;

import java.io.*;
import java.net.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RMIClient extends UnicastRemoteObject {
    static final int keepAliveTime = 5000;
    private final String rmiHost;
    private final int rmiPort;
    private final String rmiRegistryName;
    private RMIServerInterface sv;
    private Client client;

    public RMIClient(RMIServerInterface svInterface, Client client, String rmiHost, int rmiPort, String rmiRegistryName) throws RemoteException {
        super();
        this.sv = svInterface;
        this.client = client;
        this.rmiHost = rmiHost;
        this.rmiPort = rmiPort;
        this.rmiRegistryName = rmiRegistryName;
    }

    public static void main(String[] args) {
        System.getProperties().put("java.security.policy", "policy.all");
        String rmiHost;
        int rmiPort;

        String rmiRegistryName;

        String SETTINGS_PATH = "src\\RMIClient.properties";

        try {
            InputStream config = new FileInputStream(SETTINGS_PATH);
            Properties prop = new Properties();
            prop.load(config);

            rmiHost = prop.getProperty("RMI_HOST");
            rmiPort = Integer.parseInt(prop.getProperty("RMI_PORT"));
            rmiRegistryName = prop.getProperty("RMI_REGISTRY_NAME");

            if (rmiHost == null || rmiPort == 0 || rmiRegistryName == null) {
                System.out.println("[EXCEPTION] Properties file is missing some properties");
                System.out.println("[EXCEPTION] Current config: " + rmiHost + ":" + rmiPort + " " + rmiRegistryName);
                return;
            }

            // GET SERVER INTERFACE USING REGISTRY
            RMIServerInterface svInterface = (RMIServerInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegistryName);

            System.out.println("[CLIENT] Connected to server: " + rmiHost + ":" + rmiPort + " " + rmiRegistryName + "");


            Client client = new Client("Anon", false);
            RMIClient rmi_client = new RMIClient(svInterface, client, rmiHost, rmiPort, rmiRegistryName);
            rmi_client.menu();
        } catch (RemoteException e) {
            System.out.println("[CLIENT] RemoteException");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("[CLIENT] IOException");
            e.printStackTrace();
        } catch (NotBoundException e) {
            System.out.println("[CLIENT] NotBoundException");
            e.printStackTrace();
        }
    }

    private void printMenus(int type) {
        switch (type) {
            case 0:
                // Login or Register
                System.out.print("\n### Login Menu ###\n1.Search words\n2.Search Link\n  3.Login\n  4.Register\n  e.Exit\n --> Choice: ");
                return;
            case 1:
                // admin - main menu
                System.out.print("\n### Admin User Panel ###\n1.Search words\n2.Search Link\n3.Index new URL\n4.User List\n5.Give admin Perms\n6.Logout\n  e.Exit\n --> Choice: ");
                return;
            case 2:
                // user - main menu
                System.out.print("\n### User Panel ###\n1.Search words\n2.Search Link\n  3.Logout\n  e.Exit\n --> Choice: ");
                return;
            case 3:
                System.out.print("\n### Admin Panel ###\n1.Top 10 pages\n2.Top 10 searches\n3.Multicast Servers\nb.Back\n  e.Exit\n --> Choice: ");
                return;
            default:
                System.out.println("[EXCEPTION] Invalid menu type");
                // exit program
                System.exit(1);
        }
    }

    private void menu() {
        InputStream in = System.in;
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        // create a new client object
        this.client = new Client("Anon", false);

        try {
            while (true) {
                // Anonymous user, not logged yet
                if (this.client.username.equals("Anon")) {
                    // Login or Register
                    printMenus(0);
                    boolean shouldstop = anonLogic(br);
                    if (!shouldstop) {
                        System.out.println("[CLIENT] Exiting...");
                        System.exit(0);
                    }

                } else {
                    if (this.client.admin) {
                        // Admin user
                        printMenus(1);
                        boolean shouldstop = adminLoggedLogic(br);
                        if (!shouldstop) {
                            return;
                        }
                    } else {
                        // Normal user
                        printMenus(2);
                        boolean shouldstop = loggedLogic(br);
                        if (!shouldstop) {
                            return;
                        }
                    }

                }
            }
        } catch (ConnectException e) {
            System.out.println("[EXCEPTION] ConnectException: " + e.getMessage());
            serverErrorHandling();
        } catch (RemoteException e) {
            System.out.println("[EXCEPTION] RemoteException: " + e.getMessage());
            // e.printStackTrace();
            serverErrorHandling();
        } catch (IOException e) {
            System.out.println("[EXCEPTION] IOException: " + e.getMessage());
            serverErrorHandling();
        }
    }

    private boolean loggedLogic(BufferedReader br) throws IOException {
        String choice = "";

        try {
            choice = br.readLine();
        } catch (IOException ei) {
            System.out.println("EXCEPTION: IOException");
            return false;
        }


        switch (choice.toLowerCase()) {
            case "1":
                // Search words
                searchImportance(br);
                break;
            case "2":
                // Search Link
                searchLinks(br);
                break;
            case "3":
                logout();
                break;
            case "e":
                // Exit
                System.out.println("[CLIENT] Exiting...");
                System.exit(0);
                break;
            default:
                System.out.println("[CLIENT] Invalid choice");
                break;
        }
        return true;
    }

    private boolean adminLoggedLogic(BufferedReader br) throws IOException {
        String choice = "";

        try {
            choice = br.readLine();
        } catch (IOException ei) {
            System.out.println("EXCEPTION: IOException");
            return false;
        }

        switch (choice.toLowerCase()) {
            case "1":
                // Search words
                searchImportance(br);
                break;
            case "2":
                // Search Link
                searchLinks(br);
                break;
            case "3":
                // Index new URL
                indexNewURL(br);
                break;
            case "4":
                // User List
                userList();
                break;
            case "5":
                // Give admin perms
                giveAdminPerms(br);
                break;
            case "6":
                // history
                history();
                break;
            case "7":
                // Logout
                logout();
                break;
            case "e":
                // Exit
                System.out.println("[CLIENT] Exiting...");
                System.exit(0);
                break;
            default:
                System.out.println("[CLIENT] Invalid choice");
                break;
        }
        return true;
    }

    private void giveAdminPerms(BufferedReader br) {
    }

    private void history() throws RemoteException {
        ArrayList<String> res = this.sv.history(this.client.username);

        if (res == null) {
            System.out.println("[CLIENT] No history found");
            return;
        }

        String status = res.get(0);
        if (status.equals("failure")) {
            String msg = res.get(1);
            System.out.println("[CLIENT] Couldn't fetch history: " + msg);
            return;
        }

        if (res.size() == 1) {
            System.out.println("[CLIENT] No history found");
            return;
        }

        System.out.println("[CLIENT] History:");

        for (int i = 1; i < res.size(); i++) {
            System.out.println(res.get(i));
        }

    }

    private void userList() {

    }

    private void indexNewURL(BufferedReader br) {

    }

    private Boolean anonLogic(BufferedReader br) throws IOException {
        String choice = "";

        try {
            choice = br.readLine();
        } catch (IOException ei) {
            System.out.println("EXCEPTION: IOException");
            return false;
        }

        switch (choice.toLowerCase()) {
            case "1":
                // Search words
                searchImportance(br);
                break;
            case "2":
                // Search Link
                searchLinks(br);
                break;
            case "3":
                // Login
                login(br);
                break;
            case "4":
                // Register
                register(br);
                break;
            case "e":
                // Exit
                System.out.println("[CLIENT] Exiting...");
                System.exit(0);
                break;
            default:
                System.out.println("[CLIENT] Invalid choice");
                break;
        }
        return true;
    }

    private void searchImportance(BufferedReader br) throws RemoteException {
        System.out.print("\nSearch by Importance: ");
        String phraseSearch = "";

        while (true) {
            try {
                phraseSearch = br.readLine();
                if (phraseSearch.contains(":") || phraseSearch.contains("|")) {
                    System.out.print("[CLIENT] Word cannot contain ':' or '|'\nWord: ");
                    continue;
                }
                break;
            } catch (IOException e) {
                System.out.println("[EXCEPTION] IOException");
                e.printStackTrace();
            }
        }

        ArrayList<String> links = this.sv.searchWord(phraseSearch);
        if (links.size() == 0) {
            System.out.println("[CLIENT] No Words found");
            return;
        }
        // printLinks("LINKS", links, true);
    }

    private void searchLinks(BufferedReader br) throws RemoteException {
        String phrase = ""; // search phrase
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9]*$");
        String[] pois = {"de", "sobre", "a", "o", "que", "e", "do", "da", "em", "um", "para", "é", "com", "não", "uma", "os", "no", "se", "na", "por", "mais", "as", "dos", "como", "mas", "foi", "ao", "ele", "das", "tem", "à", "seu", "sua", "ou", "ser", "quando", "muito", "há", "nos", "já", "está", "eu", "também", "só", "pelo", "pela", "até", "isso", "ela", "entre", "era", "depois", "sem", "mesmo", "aos", "ter", "seus", "quem", "nas", "me", "esse", "eles", "estão", "você", "tinha", "foram", "essa", "num", "nem", "suas", "meu", "às", "minha", "têm", "numa", "pelos", "elas", "havia", "seja", "qual", "será", "nós", "tenho", "lhe", "deles", "essas", "esses", "pelas", "este", "fosse", "dele", "tu", "te", "vocês", "vos", "lhes", "meus", "minhas", "teu", "tua", "teus", "tuas", "nosso", "nossa", "nossos", "nossas", "dela", "delas", "esta", "estes", "estas", "aquele", "aquela", "aqueles", "aquelas", "isto", "aquilo", "estou", "está", "estamos", "estão", "estive", "esteve", "estivemos", "estiveram", "estava", "estávamos", "estavam", "estivera", "estivéramos", "esteja", "estejamos", "estejam", "estivesse", "estivéssemos", "estivessem", "estiver", "estivermos", "estiverem", "hei", "há", "havemos", "hão", "houve", "houvemos", "houveram", "houvera", "houvéramos", "haja", "hajamos", "hajam", "houvesse", "houvéssemos", "houvessem", "houver", "houvermos", "houverem", "houverei", "houverá", "houveremos", "houverão", "houveria", "houveríamos", "houveriam", "sou", "somos", "são", "era", "éramos", "eram", "fui", "foi", "fomos", "foram", "fora", "fôramos", "seja", "sejamos", "sejam", "fosse", "fôssemos", "fossem", "for", "formos", "forem", "serei", "será", "seremos", "serão", "seria", "seríamos", "seriam", "tenho", "tem", "temos", "tém", "tinha", "tínhamos", "tinham", "tive", "teve", "tivemos", "tiveram", "tivera", "tivéramos", "tenha", "tenhamos", "tenham", "tivesse", "tivéssemos", "tivessem", "tiver", "tivermos", "tiverem", "terei", "terá", "teremos", "terão", "teria", "teríamos", "teriam"};
        ArrayList<String> stopWords = new ArrayList<>(Arrays.asList(pois));

        System.out.print("\nSearch: ");

        while (true) {
            try {
                phrase = br.readLine();
                if (phrase.contains("|") || phrase.contains(":")) {
                    System.out.print("[CLIENT] Word cannot contain '|' or ':'\nLink: ");
                    continue;
                }
                Matcher matcher = pattern.matcher(phrase);
                if (!matcher.matches()) {
                    System.out.print("[CLIENT] Word cannot contain special characters\nLink: ");
                    continue;
                }
                break;
            } catch (IOException e) {
                System.out.println("[EXCEPTION] IOException");
                e.printStackTrace();
            }
        }

        // separate words by space
        String[] words = phrase.split(" ");

        // remove stop words
        for (int i = 0; i < words.length; i++) {
            if (stopWords.contains(words[i])) {
                words[i] = "";
            }
        }

        HashMap<String, ArrayList<String>> res = this.sv.searchLinks(words);

        // check for empty results
        if (res.size() == 0) {
            System.out.println("[CLIENT] No Links found");
            return;
        }

        printLinks(res, br);
    }

    private void printLinks(HashMap<String, ArrayList<String>> links, BufferedReader br) {
        // links will be like this: <link, <title, description>>

        // print links from 1 to 10 and ask if the user wants to see more,
        // if so print the next 10
        int i = 0;
        System.out.println("\n### RESULTS ###");
        for (String link : links.keySet()) {
            i++;
            System.out.println("  " + i + " - " + links.get(link).get(0));
            System.out.println("    " + link);
            System.out.println("    " + links.get(link).get(1));
            System.out.println();
            if (i == 10) {
                System.out.print("Do you want to see more? (y/n): ");
                try {
                    String answer = br.readLine();
                    if (answer.equals("n")) {
                        break;
                    }
                } catch (IOException e) {
                    System.out.println("[EXCEPTION] IOException");
                    e.printStackTrace();
                }
                i = 0;
            }
        }
    }

    private void login(BufferedReader br) throws RemoteException {
        String username = "";
        String password = "";

        while (username.equals("") || password.equals("")) {
            try {
                System.out.print("\n### LOGIN ###\n  Username: ");
                username = br.readLine();

                while (username.length() < 4 || username.length() > 20) {
                    System.out.print("[CLIENT] Username must be between 4 and 20 characters\n  Username: ");
                    username = br.readLine();
                }

                System.out.print("  Password: ");
                password = br.readLine();

                while (password.length() < 4 || password.length() > 20) {
                    System.out.print("[CLIENT] Password must be between 4 and 20 characters\n  Password: ");
                    password = br.readLine();
                }
            } catch (IOException e) {
                System.out.println("[EXCEPTION] IOException");
                e.printStackTrace();
            }

            ArrayList<String> checked = this.sv.checkLogin(username, password);
            if (checked.get(0).equals("true")) {
                boolean admin = Boolean.getBoolean(checked.get(1));
                this.client = new Client(username, Boolean.getBoolean(checked.get(1)));

                if (admin) {
                    System.out.println("[CLIENT] Login successful as admin");
                } else {
                    System.out.println("[CLIENT] Login successful");
                }

                return;
            } else {
                System.out.println("[CLIENT] Login failed: " + checked.get(2));
                String choice = "";
                while (!choice.equals("y") && !choice.equals("n")) {
                    System.out.print("[CLIENT] Try again? (y/n): ");
                    try {
                        choice = br.readLine();
                    } catch (IOException e) {
                        System.out.println("[EXCEPTION] IOException");
                        e.printStackTrace();
                    }
                }
                if (choice.equals("n")) {
                    return;
                }
            }
        }
    }

    private void register(BufferedReader br) throws RemoteException {
        String username = "", password = "", firstName = "", lastName = "";
        while (true) {
            // get username and password
            try {
                System.out.print("\n### REGISTER ###\n  Username: ");
                username = br.readLine();
                while (username.length() < 4 || username.length() > 20 || username.equals("Anon")) {
                    System.out.print("[CLIENT] Username must be between 4 and 20 characters and it can't be Anon\n  Username: ");
                    username = br.readLine();
                }

                System.out.print("  Password: ");
                password = br.readLine();
                while (password.length() < 4 || password.length() > 20) {
                    System.out.print("[CLIENT] Password must be between 4 and 20 characters\n  Password: ");
                    password = br.readLine();
                }

                System.out.print("  First Name: ");
                firstName = br.readLine();
                while (firstName.length() < 1) {
                    System.out.println("[CLIENT] First name must be at least 1 character\n\n  First Name: ");
                    firstName = br.readLine();
                }

                System.out.print("  Last Name: ");
                lastName = br.readLine();
                while (lastName.length() < 1) {
                    System.out.println("[CLIENT] Last name must be at least 1 character\n\n  Last Name: ");
                    lastName = br.readLine();
                }

            } catch (IOException e) {
                System.out.println("[EXCEPTION] IOException");
                e.printStackTrace();
            }

            // System.out.println("[CLIENT] Registering: " + username + " " + password + " " + firstName + " " + lastName + "");

            ArrayList<String> res = this.sv.checkRegister(username, password, firstName, lastName);

            if (res.get(0).equals("true")) {
                // register success
                System.out.println("\n[CLIENT] Registration success!");

                // admin or not
                this.client = new Client(username, res.get(1).equals("true"));
                this.sv.updateClient(this.client.username, this.client);


                System.out.println("[CLIENT] Logged in as " + this.client.username);
                return;
            } else {
                System.out.println("[ERROR] Registration failed: " + res.get(2));
                System.out.print("[CLIENT] Try again? (y/n): ");
                try {
                    String choice = br.readLine();
                    while (!choice.equals("y") && !choice.equals("n")) {
                        System.out.println("[CLIENT] Invalid choice");
                        System.out.print("[CLIENT] Try again? (y/n): ");
                        choice = br.readLine();
                    }
                    if (choice.equals("n")) {
                        return;
                    }
                } catch (IOException e) {
                    System.out.println("[EXCEPTION] IOException");
                    e.printStackTrace();
                }
            }
        }
    }

    private void logout() throws RemoteException {
        this.sv.updateClient(this.client.username, null);
        this.client = new Client("Anon", false);
        System.out.println("[CLIENT] Logged out");
    }

    private void serverErrorHandling() {
        System.out.println("[EXCEPTION] Could not connect to server");
        while (true) {
            try {
                System.out.println("[CLIENT] Trying to reconnect...");
                Thread.sleep(keepAliveTime);
                this.sv = (RMIServerInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegistryName);
                this.sv.updateClient(this.client.username, this.client);

                System.out.println("[CLIENT] Reconnected!");
                this.menu();
                break;
            } catch (RemoteException | NotBoundException | InterruptedException e1) {
                System.out.println("[EXCEPTION] Could not connect to server: " + e1.getMessage());
            }
        }
    }

}
