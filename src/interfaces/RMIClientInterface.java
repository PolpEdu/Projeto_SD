package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIClientInterface extends Remote{
    void notifyMessage(String message) throws RemoteException;
}
