package com.ProjetoSD.SearchEngine;

import com.ProjetoSD.Client.User;
import com.ProjetoSD.interfaces.RMIBarrelInterface;

import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * Classe indexBarrel que implementa a interface RMIBarrelInterface
 */
public class IndexBarrel extends UnicastRemoteObject implements RMIBarrelInterface {
    /**
     * number of times to check if the barrel is alive
     */
    static final int alive_checks = 5;
    /**
     * time to wait between alive checks
     */
    static final int await_time = 2000;
    /**
     * interface of the barrel
     */
    RMIBarrelInterface b;
    /**
     * port of the RMI
     */
    public int PORT;
    /**
     * host ip of the RMI
     */
    public String HOST;
    /**
     * name of the RMI
     */
    public String RMI_REGISTER;
    /**
     * list of barrel threads
     */
    private ArrayList<Barrel> barrels_threads;
    /**
     * id do indexBarrel
     */
    private int id;

    /**
     * construtor da classe IndexBarrel
     * @param id id do indexBarrel
     * @param HOST host ip do RMI
     * @param PORT port do RMI
     * @param RMI_REGISTER nome do RMI
     * @throws RemoteException
     */
    public IndexBarrel(int id, String HOST, int PORT, String RMI_REGISTER) throws RemoteException {
        super();
        // create a list of barrel threads
        this.barrels_threads = new ArrayList<>();
        this.id = id;

        this.HOST = HOST;
        this.PORT = PORT;
        this.RMI_REGISTER = RMI_REGISTER;

    }

    /**
     * aqui iniciamos a thread barrel , iniciamos o server RMI e guardamos os dados do multicast
     *
     * main do indexBarrel
     * @param args argumentos
     */

    public static void main(String[] args) {
        System.getProperties().put("java.security.policy", "policy.all");
        InputStream in = System.in;
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        try {
            Properties barrelProp = new Properties();
            barrelProp.load(new FileInputStream(new File("src/main/java/com/ProjetoSD/Barrel.properties").getAbsoluteFile()));

            Properties multicastServerProp = new Properties();
            multicastServerProp.load(new FileInputStream(new File("src/main/java/com/ProjetoSD/MulticastServer.properties").getAbsoluteFile()));

            // rmi to send register the barrel
            String rmiHost = barrelProp.getProperty("B_HOST");
            String rmiRegister = barrelProp.getProperty("B_RMI_REGISTER");
            int rmiPort = Integer.parseInt(barrelProp.getProperty("B_PORT"));

            // Multicast to receive data from downloaders
            String multicastAddress = multicastServerProp.getProperty("MC_ADDR");
            int receivePort = Integer.parseInt(multicastServerProp.getProperty("MC_RECEIVE_PORT"));
            System.out.println("GIVE ID");
            int id = Integer.parseInt(br.readLine());
            IndexBarrel mainBarrel = new IndexBarrel(id, rmiHost, rmiPort, rmiRegister);
            try {
                // create the registry
                Registry r = LocateRegistry.createRegistry(rmiPort);
                System.setProperty("java.rmi.server.hostname", rmiHost); // set the host name

                // parrel interface to rebind the barrel
                r.rebind(rmiRegister, mainBarrel); // main barrel to receive the register
                System.out.println("[BARREL-INTERFACE] BARREL RMI registry created on: " + rmiHost + ":" + rmiPort + "->" + rmiRegister);

            } catch (RemoteException e) {
                System.out.println("[BARREL-INTERFACE] RemoteException, could not create registry. Retrying in " + await_time / 1000 + " second...");

                try {
                    Thread.sleep(await_time);
                    mainBarrel.b = (RMIBarrelInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegister);
                    mainBarrel.backUp(rmiPort, rmiHost, rmiRegister);
                } catch (InterruptedException | NotBoundException | RemoteException ei) {
                    System.out.println("[EXCEPTION] InterruptedException | NotBoundException | RemoteException");
                    ei.printStackTrace();
                }
            }

            Semaphore ackSem = new Semaphore(1);
            for (int i = 1; i < 2; i++) {

                if (rmiHost == null || rmiPort == 0 || rmiRegister == null || multicastAddress == null || receivePort == 0) {
                    System.out.println("[BARREL " + i + "] Error reading properties file");
                    System.exit(1);
                }

                File linkfile = new File("src/main/java/com/ProjetoSD/links-" + i);
                File wordfile = new File("src/main/java/com/ProjetoSD/words-" + i);
                File infofile = new File("src/main/java/com/ProjetoSD/info-" + i);

                Database files = new Database();
                Barrel barrel_t = new Barrel(id ,receivePort, multicastAddress,  linkfile, wordfile, infofile, files, ackSem);
                mainBarrel.barrels_threads.add(barrel_t);
                barrel_t.start();
            }
        } catch (RemoteException e) {
            System.out.println("[BARREL] Error creating registry: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("[BARREL] Error reading properties file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Caso o registry do RMI nao seja criado, esta função é chamada para recriar o registry apos um certo tempo
     *
     * @param rmiPort port do RMI
     * @param rmiHost host do RMI
     * @param rmiRegister nome do RMI
     * @throws RemoteException
     */
    private void backUp(int rmiPort, String rmiHost, String rmiRegister) throws RemoteException {
        while (true) {
            try {
                Barrel barrel_t = selectBarrelToExcute();

                if (this.b.alive()) {
                    System.out.println("[BARREL] Connection to RMI server reestablished");
                    break;
                }
            } catch (RemoteException e) {
                System.out.println("[BARREL] RemoteException, Getting connection, retrying in " + await_time / 1000 + " second(s)...");
                for (int i = 0; i < alive_checks; i++) {
                    try {
                        Thread.sleep(await_time);
                        this.b = (RMIBarrelInterface) LocateRegistry.getRegistry(rmiHost, rmiPort).lookup(rmiRegister);
                    } catch (RemoteException er) {
                        System.out.println("[EXCEPTION] RemoteException, could not create registry. Retrying in " + +await_time / 1000 + " second(s)...");
                        this.b = null;
                    } catch (InterruptedException ei) {
                        System.out.println("[EXCEPTION] InterruptedException");
                        ei.printStackTrace();
                        return;
                    } catch (NotBoundException en) {
                        System.out.println("[EXCEPTION] NotBoundException");
                        en.printStackTrace();
                        return;
                    }
                }
            }
        }
    }

    /**
     * Esta função é chamada para selecionar um barril para dizer se está vivo ou não
     *
     * @return barrel alive
     * @throws RemoteException
     */
    @Override
    public boolean alive() throws RemoteException {
        Barrel barrel = this.selectBarrelToExcute();
        return barrel != null;
    }

    /**
     *
     * Chama a função para selecionar um barril para executar a função
     * Esta função é chamada para registar um utilizador e o guardar no ficheiro de users
     *
     * @param username recebe o username
     * @param password recebe a password
     * @param firstName recebe o primeiro nome
     * @param lastName recebe o ultimo nome
     * @return arraylist com o status e a mensagem
     * @throws RemoteException
     */

    @Override
    public ArrayList<String> checkUserRegistration(String username, String password, String firstName, String lastName) throws RemoteException {
        // get the barrel to register the user
        Barrel barrel = this.selectBarrelToExcute();
        if (barrel == null) {
            // "status:failure | message:No barrels available"
            return new ArrayList<>(Arrays.asList("failure", "No barrels available"));
        }

        HashMap<String, User> users = barrel.files.getUsers();
        if (users.containsKey(username)) {
            // "status:failure | message:User already exists"
            return new ArrayList<>(Arrays.asList("failure", "User already exists"));
        }

        // if no users, make first user admin
        if (users.size() == 0) {
            // "status:success | message:User registered"
            users.put(username, new User(username, password, true, firstName, lastName));
            barrel.files.updateUsers(users);
            return new ArrayList<>(Arrays.asList("success", "Admin User registered", "true"));
        }

        // "status:success | message:User registered"
        users.put(username, new User(username, password, false, firstName, lastName));
        barrel.files.updateUsers(users);
        return new ArrayList<>(Arrays.asList("success", "User registered", "false"));
    }

    /**
     * Chama a função para selecionar um barril para executar a função
     * verficar se o user existe e se a password está correta
     *
     * @param username recebe o username
     * @param password recebe a password
     * @return arraylist com o status e a mensagem
     * @throws RemoteException
     */
    public ArrayList<String> verifyUser(String username, String password) throws RemoteException {
        // get the barrel to register the user
        Barrel barrel = this.selectBarrelToExcute();
        if (barrel == null) {
            // "status:failure | message:No barrels available"
            return new ArrayList<>(Arrays.asList("failure", "No barrels available"));
        }

        HashMap<String, User> users = barrel.files.getUsers();
        if (!users.containsKey(username)) {
            // "status:failure | message:User does not exist"
            return new ArrayList<>(Arrays.asList("failure", "User does not exist"));
        }

        User user = users.get(username);
        if (!user.password.equals(password)) {
            // "status:failure | message:Wrong password"
            return new ArrayList<>(Arrays.asList("failure", "Wrong password"));
        }

        // "status:success | message:User verified"
        return new ArrayList<>(Arrays.asList("success", "User verified", Boolean.toString(user.admin)));
    }

    /**
     *chama a função para selecionar um barril para executar a função
     * procura os links associados a uma palavra
     *
     * @param word recebe a palavra
     * @return links associados a palavra
     * @throws RemoteException
     */

    @Override
    public HashSet<String> searchLinks(String word) throws RemoteException {
        Barrel barrel = this.selectBarrelToExcute();
        if (barrel == null) {
            // "status:failure | message:No barrels available"
            // return an Hashset with status and message
            return new HashSet<>(Arrays.asList("failure","No barrels available"));
        }

        return barrel.getLinksAssciatedWord(word);
    }

    /**
     * chama a função para selecionar um barril para executar a função
     * retorna o titulo do link caso exista, caso contrario retorna que o link nao existe
     *
     * @param word recebe a palavra
     * @return titulo do link
     * @throws RemoteException
     */
    @Override
    public ArrayList<String> searchTitle(String word) throws RemoteException {
        if (word == null) {
            // "status:failure | message:Word is null"
            return new ArrayList<>(Arrays.asList("failure", "Word is null"));
        }

        Barrel barrel = this.selectBarrelToExcute();
        if (barrel == null) {
            // "status:failure | message:No barrels available"
            return new ArrayList<>(Arrays.asList("failure", "No barrels available"));
        }

        // System.out.println("[BARREL-INTERFACE] Searching for title: " + word);
        String title = barrel.getLinkTitle(word);

        // System.out.println("[BARREL-INTERFACE] Links title: " + title);

        if (title == null) {
            // "status:failure | message:Link does not exist"
            return new ArrayList<>(Arrays.asList("failure", "Link does not exist"));
        }

        // return an array list with the first element as the status
        // and the rest with links
        ArrayList<String> result = new ArrayList<>();
        result.add("success");
        result.add(title);
        return result;
    }

    /**
     * pede a um barril para executar a função
     * retorna o titulo do link caso exista, caso contrario retorna que o link nao existe
     *
     * @param word recebe a palavra
     * @return descricao do link
     * @throws RemoteException
     */
    @Override
    public ArrayList<String> searchDescription(String word) throws RemoteException {
        if (word == null) {
            // "status:failure | message:Word is null"
            return new ArrayList<>(Arrays.asList("failure", "Word is null"));
        }

        Barrel barrel = this.selectBarrelToExcute();
        if (barrel == null) {
            // "status:failure | message:No barrels available"
            return new ArrayList<>(Arrays.asList("failure", "No barrels available"));
        }

        String linksInfo = barrel.getLinkDescription(word);
        // System.out.println("[BARREL-INTERFACE] Links description: " + linksInfo);
        if (linksInfo == null) {
            // "status:failure | message:Link does not exist"
            return new ArrayList<> (Arrays.asList("failure", "Link does not exist"));
        }
        // return links linksInfo with ArrayList
        ArrayList<String> result = new ArrayList<>();
        result.add("success");
        result.add(linksInfo);
        return result;
    }

    /**
     * pede a um barril para executar a função
     * devolve os links onde o link aparece caso exista, caso contrario retorna um hashset vazio
     *
     * @param link recebe o link
     * @return devolve links onde o link aparece
     * @throws RemoteException
     */
    @Override
    public HashSet<String> linkpointers(String link) throws RemoteException {
        Barrel barrel = this.selectBarrelToExcute();
        if (barrel == null) {
            // "status:failure | message:No barrels available"
            return new HashSet<>(Arrays.asList("failure", "No barrels available"));
        }

        // System.out.println("[BARREL-INTERFACE] Searching for linkpointers: " + link);

        return barrel.getLinkPointers(link);
    }

    /**
     * pede a um barril para executar a função
     * devolve se o user é admin ou nao atraves de um boolean
     *
     * @param username recebe o username
     * @return true se for admin, false se nao for
     * @throws RemoteException
     */
    @Override
    public boolean isAdmin(String username) throws RemoteException {
        Barrel barrel = this.selectBarrelToExcute();
        if (barrel == null) {
            // "status:failure | message:No barrels available"
            return false;
        }

        HashMap<String, User> users = barrel.files.getUsers();
        if (!users.containsKey(username)) {
            // "status:failure | message:User does not exist"
            return false;
        }

        User user = users.get(username);
        return user.admin;
    }

    /**
     * pede aos barris para executar a função e devolve os barris que estao vivos
     *
     * @return lista de barris vivos
     * @throws RemoteException
     */
    @Override
    public ArrayList<ArrayList<String>> getBarrelsAlive() throws RemoteException {
        // return the current ip port and status of the barrels
        ArrayList<ArrayList<String>> result = new ArrayList<>();
        /*for (Barrel barrel : this.barrels_threads) {
            ArrayList<String> barrelInfo = new ArrayList<>();

            result.add(barrelInfo);
        }*/
        return result;
    }

    /**
     * pede a um barril para executar a função
     * coloca a palavra na lista de palavras mais pesquisadas com o numero de vezes que foi pesquisada
     * devolve se a palavra foi guardada
     *
     * @param phrase recebe uma pesquisa de um user
     * @return devolve se a palavra foi guardada
     * @throws RemoteException
     */
    @Override
    public ArrayList<String> saveWordSearches(String phrase) throws RemoteException {
        Barrel barrel = this.selectBarrelToExcute();
        if (barrel == null) {
            // "status:failure | message:No barrels available"
            return new ArrayList<>(Arrays.asList("failure", "No barrels available"));
        }

        HashMap<String, Integer> topWords = barrel.files.getTop10Searches();
        if (topWords.containsKey(phrase)) {
            topWords.put(phrase, topWords.get(phrase) + 1);
        } else {
            topWords.put(phrase, 1);
        }

        barrel.files.updateTopWords(topWords);
        return new ArrayList<>(Arrays.asList("success", "Word saved"));
    }

    /**
     * pede a um barril para executar a função
     * devolve as 10 palavras mais pesquisadas que estao num ficheiro
     *
     * @return devolve as 10 palavras mais pesquisadas
     * @throws RemoteException
     */
    @Override
    public HashMap<String, Integer> getTop10Searches() throws RemoteException {
        Barrel barrel = this.selectBarrelToExcute();
        if (barrel == null) {
            // "status:failure | message:No barrels available"
            return null;
        }

        return barrel.files.getTop10Searches();
    }

    /**
     * seleciona o barril que vai executar a função de forma aleatoria dentro dos barris que estao vivos
     * se nao existirem barris ativos devolve null
     *
     * @return devolve o barril que vai executar a função
     */

    private Barrel selectBarrelToExcute() {
        // select a random barrel to fulfill the task
        if (this.barrels_threads.size() == 0) {
            System.out.println("[BARREL-INTERFACE] No barrels to fulfill the task");
            // no barrels to fulfill the task
            return null;
        }

        int random = (int) (Math.random() * this.barrels_threads.size());

        // check if barrel is alive if not remove from barrels_threads and select another barrel
        if (!this.barrels_threads.get(random).isAlive()) {
            System.out.println("[BARREL-INTERFACE] Barrel " + random + " is not alive");
            this.barrels_threads.remove(random);
            return this.selectBarrelToExcute();
        }

        return this.barrels_threads.get(random);
    }
}
