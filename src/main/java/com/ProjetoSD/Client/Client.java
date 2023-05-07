package com.ProjetoSD.Client;

import java.io.Serializable;

/**
 * Client class that represents a client in a connection.
 * It stores the username and admin status of the user.
 * This class is used primarily to keep track of connections and authentication.
 */
public class Client implements Serializable {
    /**username and password of the user*/
    public String username;

    /**admin status of the user*/
    public boolean admin;

    /**
     * Constructor for the Client class that takes in a username and admin status.
     *
     * @param username username of the user
     * @param admin admin status of the user
     */
    public Client(String username,boolean admin){
        this.username = username;
        this.admin = admin;
    }
}
