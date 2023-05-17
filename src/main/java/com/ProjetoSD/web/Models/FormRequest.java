package com.ProjetoSD.web.Models;

/** This class represents a form request. */
public class FormRequest {
    /** The username. */
    private String username;
    /** The password. */
    private String password;

    /** Default constructor. */
    public FormRequest() {
    }

    /**
     * Constructor.
     *
     * @param username The username.
     * @param password The password.
     */
    public FormRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }


    /**
     * Gets the username.
     *
     * @return The username.
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Gets the password.
     *
     * @return The password.
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Sets the username.
     *
     * @param username The username.
     */
    public void setUsername(String username) {
        this.username = username;
    }
    /**
     * Sets the password.
     *
     * @param password The password.
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
