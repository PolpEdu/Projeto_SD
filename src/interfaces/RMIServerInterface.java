package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import Client.*;

public interface RMIServerInterface extends Remote {
    int alive() throws RemoteException;
    void updateClient(String username, Client client) throws RemoteException;

    ArrayList<String> checkLogin(String username, String password) throws RemoteException;
    ArrayList<String> checkRegister(String username, String password, String firstName, String lastName) throws RemoteException;
}