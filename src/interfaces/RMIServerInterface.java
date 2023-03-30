package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

public interface RMIServerInterface extends Remote {
    boolean alive() throws RemoteException;


    String takeLink() throws RemoteException;
    void offerLink(String link) throws RemoteException;
    boolean isempty() throws RemoteException;

    boolean isLoggedIn(String username) throws RemoteException;
    ArrayList<String> checkLogin(String username, String password) throws RemoteException;
    ArrayList<String> checkRegister(String username, String password, String firstName, String lastName) throws RemoteException;

    HashMap<String, ArrayList<String>> searchLinks(String[] words) throws RemoteException;
    ArrayList<String> linkPointers(String link) throws RemoteException;

    boolean logout(String username) throws RemoteException;

    ArrayList<String> getLinksByRelevance(String link) throws RemoteException;

    boolean isAdmin(String username) throws RemoteException;

}