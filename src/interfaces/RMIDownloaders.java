package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import Client.*;

public interface RMIDownloaders extends Remote{
    String takeLink() throws RemoteException;
    void offerLink(String link);
}
