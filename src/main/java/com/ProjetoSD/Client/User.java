package com.ProjetoSD.Client;

import java.io.Serializable;

/**
 * User Class that represents a user in the database of the server.
 * It stores the username, password, admin status, name and surname of the user.
 */
public class User implements Serializable {
    /** username and password of the user */
    public String username;

    /** password of the user */
    public String password;

    /** name of the user */
    public String name;

    /** surname of the user */
    public String surname;

    /** admin status of the user */
    public boolean admin;

    /**
     * Constructor for the User class that takes in a username, password, admin status, name and surname.
     * @param username username of the user
     * @param password password of the user
     * @param admin admin status of the user
     * @param name name of the user
     * @param surname surname of the user
     */
    public User(String username, String password, boolean admin, String name, String surname) {
        this.username = username;
        this.password = password;
        this.admin = admin;
        this.name = name;
        this.surname = surname;
    }
}
