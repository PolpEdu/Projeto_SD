package com.ProjetoSD.SearchEngine;

import com.ProjetoSD.Client.Client;
import com.ProjetoSD.interfaces.RMIServerInterface;

import java.io.*;
import java.net.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RMIClient class that handles the client side of the RMI connection.
 * It handles the logic for the menu prompts and the logic for the user to interact with the server.
 */

class RMIClient {
    /**
     * time to keep the thread alive
     */
    static final int keepAliveTime = 5000;
    /**
     * rmi host address
     */
    private final String rmiHost;
    /**
     * rmi port
     */
    private final int rmiPort;
    /**
     * rmi registry name
     */
    private final String rmiRegistryName;
    /**
     * RMIserver interface
     */
    private RMIServerInterface sv;
    /**
     * client object
     */
    private Client client;

    /**
     * Constructor for RMIClient class that creates a new instance of a client
     * @param svInterface the server interface
     * @param client the client object
     * @param rmiHost the host of the server
     * @param rmiPort the port of the server
     * @param rmiRegistryName the name of the registry
     * @throws RemoteException if the client cannot connect to the server interface
     */
    public RMIClient(RMIServerInterface svInterface, Client client, String rmiHost, int rmiPort, String rmiRegistryName) throws RemoteException {
        super();
        this.sv = svInterface;
        this.client = client;
        this.rmiHost = rmiHost;
        this.rmiPort = rmiPort;
        this.rmiRegistryName = rmiRegistryName;
    }

    /**
     * Main method for the client, creates a new instance of the client
     * and connects to the server interface. It then calls the menu method
     * for the user to interact with the server
     * @param args the arguments passed to the main method. We don't use them since we already have the properties file
     */
    public static void main(String[] args) {
        System.getProperties().put("java.security.policy", "policy.all");
        String rmiHost;
        int rmiPort;

        String rmiRegistryName;

        String SETTINGS_PATH = "src/main/java/com/ProjetoSD/RMIClient.properties";

        try {
            InputStream config = new FileInputStream(SETTINGS_PATH);
            Properties prop = new Properties();
            prop.load(config);

            rmiHost = prop.getProperty("RMI_HOST");
            rmiPort = Integer.parseInt(prop.getProperty("RMI_PORT"));
            rmiRegistryName = prop.getProperty("RMI_REGISTRY_NAME");

            if (rmiHost == null || rmiPort == 0 || rmiRegistryName == null) {
                System.out.println("[EXCEPTION] Properties file is missing some properties");
                return;
            }

            System.out.println("[EXCEPTION] Current config: " + rmiHost + ":" + rmiPort + " " + rmiRegistryName);
            // GET SERVER INTERFACE USING REGISTRY
            RMIServerInterface svInterface = (RMIServerInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegistryName);
            System.out.println("[CLIENT] Connected to Search Model Server: " + rmiHost + ":" + rmiPort + " " + rmiRegistryName + "");

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


    /**
     * Method that handles the logic for all the types of menu prompts.
     * @param type the type of menu prompt: 0 - anonymous, 1 - admin, 2 - user
     */
    private void printMenus(int type) {
        switch (type) {
            case 0:
                // Login or Register
                System.out.print("\n### Login Menu ###\n1.Search Links\n2.Index New URL\n  3.Login\n  4.Register\n  e.Exit\n --> Choice: ");
                return;
            case 1:
                // admin - main menu
                System.out.print("\n### Admin Panel ###\n1.Search Links\n2.Index New URL\n3.Barrels List\n4.Downloaders List\n5.Top 10 searches\n  6.Logout\n  e.Exit\n --> Choice: ");
                return;
            case 2:
                // user - main menu
                System.out.print("\n### User Panel ###\n1.Search Links\n2.Index New URL\n  3.Logout\n  e.Exit\n --> Choice: ");
                return;
            default:
                System.out.println("[EXCEPTION] Invalid menu type");
                // exit program
                System.exit(1);
        }
    }

    /**
     * Method that handles the logic for the anonymous user menu.
     */
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

    /**
     * this function asks the RMI server for the top 10 searches and prints them for the user
     */
    private void topSearches() {
        ArrayList<String> top10 = new ArrayList<>();
        // get the top 10 searches from the server
        try {
            top10 = this.sv.getTop10Searches();
        } catch (RemoteException e) {
            System.out.println("[CLIENT] RemoteException");
            e.printStackTrace();
        }

        // print the top 10 searches
        System.out.println("\n### Top 10 Searches ###");
        for (int i = 0; i < top10.size(); i++) {
            System.out.println((i+1) + ". " + top10.get(i));
        }
    }

    private void downloadersUp() {

    }

    private void barrelsUp() {
        // print the host and the port of all the barrels


    }

    /**
     * Method that handles the logic for the loggedIn user menu.
     * @param br the buffered reader
     * @return true if no errors occurred, false otherwise
     * @throws IOException if an error occurs while reading from the buffer
     */
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
                // Search Link
                searchLinks(br);
                break;
            case "2":
                // Index new URL
                indexNewUrl(br);
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
    /**
     * Method that handles the logic for the anonymous user menu.
     * @param br the buffered reader
     * @return true if no errors occurred, false otherwise
     * @throws IOException if an error occurs while reading from the buffer
     */
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
                searchLinks(br);
                break;
            case "2":
                // Login
                indexNewUrl(br);
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

    /**
     * Method that handles the logic for the admin user menu.
     * @param br the buffered reader object
     * @return true if there wasn't an error, false otherwise
     * @throws IOException if there was an error reading from the buffer
     */
    private boolean adminLoggedLogic(BufferedReader br) throws IOException {
        String choice = "";

        try {
            choice = br.readLine();
        } catch (IOException ei) {
            System.out.println("EXCEPTION: IOException");
            return false;
        }

        // System.out.print("\n### Admin Panel ###\n1.Search Links\n2.Barrels List\n3.Downloaders List\n4.Top 10 searches\n  5.Logout\n  e.Exit\n --> Choice: ");

        switch (choice.toLowerCase()) {
            case "1":
                // Search Link
                searchLinks(br);
                break;
            case "2":
                // Index new URL
                indexNewUrl(br);
                break;
            case "3":
                // Index new URL
                barrelsUp();
                break;
            case "4":
                // Downloaders List
                downloadersUp();
                break;
            case "5":
                // User List
                topSearches();
                break;
            case "6":
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

    /**
     * this function sends a link to the URLQueue to be indexed
     * @param br the buffered reader in the commands line
     * @throws RemoteException
     */
    private void indexNewUrl(BufferedReader br) throws RemoteException {
        String url = "";
        System.out.print("\nURL: ");
        // check if the url is valid (http|ftp|https):\/\/([\w_-]+(?:(?:\.[\w_-]+)+))([\w.,@?^=%&:\/~+#-]*[\w@?^=%&\/~+#-])
        Pattern pattern = Pattern.compile("^(http|ftp|https):\\/\\/([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:\\/~+#-]*[\\w@?^=%&\\/~+#-])$");

        while (true) {
            try {
                url = br.readLine();
            } catch (IOException e) {
                System.out.println("[EXCEPTION] IOException");
                e.printStackTrace();
            }

            Matcher matcher = pattern.matcher(url);
            if (!matcher.matches()) {
                System.out.print("[CLIENT] Invalid URL\nURL: ");
                continue;
            }
            break;
        }

        boolean res = this.sv.indexNewUrl(url);

        if (res) {
            System.out.println("[CLIENT] URL indexed successfully");
        } else {
            System.out.println("[CLIENT] URL already indexed");
        }
    }

    /**
     * this functions sends a search phrase to the server to be searched and get the links
     * that are associated with the words in the search phrase
     * we also already dispose the links in order of relevance
     *
     * @param br the buffered reader in the commands line
     * @throws RemoteException
     */
    private void searchLinks(BufferedReader br) throws RemoteException {
        String phrase = ""; // search phrase
        System.out.print("\nSearch: ");

        Pattern pattern = Pattern.compile("^[a-zA-Z0-9 ]*$");

        while (true) {
            try {
                phrase = br.readLine();
                if (phrase.contains("|") || phrase.contains(":")) {
                    System.out.print("[CLIENT] Word cannot contain '|' or ':'\nLink: ");
                    continue;
                }
                Matcher matcher = pattern.matcher(phrase);
                if (!matcher.matches()) {
                    System.out.print("[CLIENT] Word cannot contain special characters\nSearch: ");
                    continue;
                }
                break;
            } catch (IOException e) {
                System.out.println("[EXCEPTION] IOException");
                e.printStackTrace();
            }
        }



        HashMap<String, ArrayList<String>> res = this.sv.searchLinks(phrase, 1);

        // check for empty results
        if (res.size() == 0) {
            System.out.println("[CLIENT] No Links found");
            return;
        }


        HashMap<String, Integer> linksRelevance = new HashMap<String, Integer>();

        for (String link : res.keySet()) {
            // get the link relevance by checking how many links are associated with it
            int relevance = this.sv.getLinksByRelevance(link).size();
            linksRelevance.put(link, relevance);
        }

        // System.out.println("relevance: " +linksRelevance);

        // this function returns a hashset of links that are associated with the link
        // the more the links are associated with the link, the more relevant the link is
        // for every link, order HashMap<String, ArrayList<String>> links by relevance

        // sort res by relevance
        HashMap<String, ArrayList<String>> linksSorted = sortByValue(res, linksRelevance);

        // System.out.println("Links Sorted: "+ linksSorted);

        boolean adm = this.sv.isAdmin(this.client.username);
        boolean isLogged = this.sv.isLoggedIn(this.client.username);

        printLinks(linksSorted, br, adm, isLogged);
    }

    /**
     * this functions put the links in an arraylist sorted by their relevance and returns it
     *
     * @param links all links that are associated with the search phrase
     * @param linksRelevance the relevance of each link
     * @return an arraylist of links sorted by relevance
     * TODO: DON'T QUERY ALL OF THE LINKS, ONLY THE FIRST 10 and then, if we want more we query the next 10
     */
    private HashMap<String, ArrayList<String>> sortByValue(HashMap<String, ArrayList<String>> links, HashMap<String, Integer> linksRelevance) {
        /*
            links: {
                <link> : [<title>, <description>],
                <link> : [<title>, <description>]
            }
            linksRelevance: {
                <link> : <relevanceWeight>,
                <link> : <relevanceWeight>
            }
         */

        // System.out.println("Links Relevance: " + linksRelevance);

        // Sort the keys of links HashMap based on the corresponding values in linksRelevance HashMap
        List<String> sortedKeys = new ArrayList<>(links.keySet());
        Collections.sort(sortedKeys, (a, b) -> linksRelevance.get(b) - linksRelevance.get(a));


        // Create a new HashMap with the sorted keys and their corresponding values
        HashMap<String, ArrayList<String>> sortedLinks = new LinkedHashMap<>();
        for (String key : sortedKeys) {
            sortedLinks.put(key, links.get(key));
        }

        //System.out.println("Sorted Links: " + sortedLinks);

        return sortedLinks;
    }

    /**
     * in this function we print the links we got from the searchLinks function and their info
     * we also print a menu if they want to see the links associated with the link
     * if the search has more then 10 links then we print them in different pages of 10 links each
     *
     * @param links the links to be printed
     * @param br the buffered reader in the commands line
     * @param isadmin if the user is admin
     * @param isLogged if the user is logged in
     * @throws RemoteException
     */
    private void printLinks(HashMap<String, ArrayList<String>> links, BufferedReader br, boolean isadmin, boolean isLogged) throws RemoteException {
        // links will be like this: <link, <title, description>>

        int i = 0;
        System.out.println("\n### RESULTS BY RELEVANCE ORDER ###");

        // create a hashmap with the index of the print and the link to help later with the link info
        HashMap<Integer, String> linksIndex = new HashMap<Integer, String>();

        for (String link : links.keySet()) {
            if (link.length() == 0) {
                System.out.println("[CLIENT] Empty link");
                continue;
            }

            i++;
            linksIndex.put(i, link);
            try {
                System.out.println("  " + i + " - " + link);
                System.out.println("    " + links.get(link).get(0));
                System.out.println("    " + links.get(link).get(1));
            } catch (IndexOutOfBoundsException e) {
                System.out.println("[EXCEPTION] Error while printing links: " + e.getMessage());
                continue;
            }

            System.out.println();
            if (i == 10) {
                while (true) {
                    // if the user is admin localy and in the server.
                    if (isLogged) {
                        System.out.print("[CLIENT] Select a link to show connections (1-10) or go to next page (y/n): ");
                    } else {
                        System.out.print("[CLIENT] Next Page? (y/n): ");
                    }
                    try {
                        String answer = br.readLine();

                        while (!answer.equals("y") && !answer.equals("n")) {

                            // check if the answer is a number between 1 and 10
                            if (isLogged) {
                                try {
                                    int linkNumber = Integer.parseInt(answer);
                                    while (linkNumber < 1 || linkNumber > 10) {
                                        System.out.print("[CLIENT] Invalid number or 'e' to exit. Select a link to show connections (1-10) or go to next page (y/n): ");
                                        answer = br.readLine();

                                        if (answer.equals("e") || answer.equals("n")) {
                                            return;
                                        } else if (answer.equals("y")) {
                                            break;
                                        }

                                        linkNumber = Integer.parseInt(answer);
                                    }
                                    // System.out.print(linksIndex);
                                    // get the link
                                    String linkToPrint = linksIndex.get(linkNumber);

                                    // get the links associated with the link
                                    ArrayList<String> linksByRelevance = this.sv.linkPointers(linkToPrint);

                                    // print the links
                                    System.out.println("\n### LINKS ASSOCIATED WITH " + linkToPrint + " ###");
                                    for (String l : linksByRelevance) {
                                        System.out.println("  " + l);
                                    }
                                    System.out.println();
                                    break;
                                } catch (NumberFormatException e) {
                                    System.out.println("[EXCEPTION] NumberFormatException");
                                    continue;
                                }
                            }

                            System.out.print("[CLIENT] Invalid answer. Do you want to see more? (y/n): ");
                            answer = br.readLine();
                        }
                        if (answer.equals("y")) {
                            break;
                        } else if (answer.equals("n")) {
                            return;
                        }
                    } catch (IOException e) {
                        System.out.println("[EXCEPTION] IOException");
                        e.printStackTrace();
                    }
                }
                i = 0;
                linksIndex = new HashMap<Integer, String>();

            }

        }
        // check if linksIndex is empty
        if (isLogged && linksIndex.size() > 0) {
            String answer = "";
            while (true) {
                System.out.print("[CLIENT] Select a link to show connections (1-" + linksIndex.size() + ") and 'e' to exit: ");
                try {
                    answer = br.readLine();

                    if (answer.equals("e")) {
                        break;
                    }

                    try {
                        int linkNumber = Integer.parseInt(answer);
                        while (linkNumber < 1 || linkNumber > linksIndex.size()) {
                            System.out.print("[CLIENT] Invalid number or 'e' to exit. Select a link to show connections (1-" + linksIndex.size() + "):");
                            answer = br.readLine();
                            linkNumber = Integer.parseInt(answer);
                        }
                        // System.out.print(linksIndex);
                        // get the link
                        String linkToPrint = linksIndex.get(linkNumber);

                        // get the links associated with the link
                        ArrayList<String> linksByRelevance = this.sv.linkPointers(linkToPrint);

                        // print the links
                        System.out.println("\n### LINKS ASSOCIATED WITH " + linkToPrint + " ###");

                        for (String l : linksByRelevance) {
                            System.out.println("  " + l);
                        }
                        System.out.println();
                        continue;
                    } catch (NumberFormatException e) {
                        System.out.println("[EXCEPTION] NumberFormatException");
                        continue;
                    }
                } catch (IOException e) {
                    System.out.println("[EXCEPTION] IOException");
                    e.printStackTrace();
                }
                return;
            }
        }
    }

    /**
     * Method to login to log the user in the server
     * here we will ask for the username and password
     * if the user fails to login 3 times, the program will ask him if he wants to try again
     * @param br BufferedReader to read the user input
     * @throws RemoteException
     */
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
            if (checked == null) {
                System.out.println("[CLIENT] Login failed: Server is down");
                return;
            }
            // System.out.println(checked);
            if (checked.get(0).equals("true")) {
                boolean admin = checked.get(1).equals("true");
                this.client = new Client(username, admin);

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

    /**
     * here we handle the registration of a new user and send the data to the server and the barrels
     *
     * @param br BufferedReader to read from the console
     * @throws RemoteException
     */
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

    /**
     *here we handle the logout of the user and pass the menu to anon mode
     * @throws RemoteException
     */

    private void logout() throws RemoteException {
        this.sv.logout(this.client.username);
        this.client = new Client("Anon", false);
        System.out.println("[CLIENT] Logged out");
    }

    /**
     * here we try to reconnect to the RMI server if the registry fails
     */
    private void serverErrorHandling() {
        System.out.println("[EXCEPTION] Could not connect to server");
        while (true) {
            try {
                System.out.println("[CLIENT] Trying to reconnect...");
                Thread.sleep(keepAliveTime);
                this.sv = (RMIServerInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegistryName);

                System.out.println("[CLIENT] Reconnected!");
                this.menu();
                break;
            } catch (RemoteException | NotBoundException | InterruptedException e1) {
                System.out.println("[EXCEPTION] Could not connect to server: " + e1.getMessage());
            }
        }
    }

}
