package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;

public interface RMIBarrelInterface extends Remote{
    boolean alive() throws RemoteException;

    ArrayList<String> checkUserRegistration(String username, String password, String firstName, String lastName) throws RemoteException;
    ArrayList<String> verifyUser(String username, String password) throws RemoteException;

    ArrayList<String> history(String username) throws RemoteException;

    HashSet<String> searchLinks(String word) throws RemoteException;
    ArrayList<String> searchTitle(String word) throws RemoteException;
    ArrayList<String> searchDescription(String word) throws RemoteException;
}