package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import Client.*;

public interface ServerInterface extends Remote {
    int alive() throws RemoteException;
}