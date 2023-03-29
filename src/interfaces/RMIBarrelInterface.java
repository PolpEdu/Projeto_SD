package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface RMIBarrelInterface extends Remote{
    boolean alive() throws RemoteException;

    ArrayList<String> checkUserRegistration(String username, String password, String firstName, String lastName) throws RemoteException;
    ArrayList<String> verifyUser(String username, String password) throws RemoteException;
    ArrayList<String> searchLink(String link) throws RemoteException;
    ArrayList<String> searchWord(String word) throws RemoteException;

}
