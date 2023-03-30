package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import Client.*;

public interface RMIServerInterface extends Remote {
    boolean alive() throws RemoteException;
    void updateClient(String username, Client client) throws RemoteException;


    String takeLink() throws RemoteException;
    void offerLink(String link) throws RemoteException;
    boolean isempty() throws RemoteException;

    ArrayList<String> checkLogin(String username, String password) throws RemoteException;
    ArrayList<String> checkRegister(String username, String password, String firstName, String lastName) throws RemoteException;

    ArrayList<String> searchLink(String link) throws RemoteException;
    ArrayList<String> searchWord(String word) throws RemoteException;

    ArrayList<String> history(String username) throws RemoteException;
}