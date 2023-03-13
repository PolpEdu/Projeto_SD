package Client;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

/*
public interface ClientInterface extends Remote {
    public void notifyClient(String message) throws RemoteException;
}
*/

public class Client implements Serializable {
    public String username;
    public boolean admin;

    public Client(String username,boolean admin){
        this.username = username;
        this.admin = admin;
    }
}
