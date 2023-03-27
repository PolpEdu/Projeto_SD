package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface RMIDownloaders extends Remote{
    String takeLink() throws RemoteException;
    void offerLink(String link) throws RemoteException;
}
