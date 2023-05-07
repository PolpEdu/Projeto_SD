package com.ProjetoSD.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

public interface RMIUrlQueueInterface extends Remote {
    String takeLink() throws RemoteException;
    void offerLink(String link) throws RemoteException;
    boolean isempty() throws RemoteException;
}

