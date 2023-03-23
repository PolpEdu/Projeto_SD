package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientInterface extends Remote{
    void notifyMessage(String message) throws RemoteException;
}
