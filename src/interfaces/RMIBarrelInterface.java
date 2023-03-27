package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIBarrelInterface extends Remote {
    boolean alive() throws RemoteException;

}
