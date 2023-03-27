package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIBarrels extends Remote {
    int alive() throws RemoteException;
}
