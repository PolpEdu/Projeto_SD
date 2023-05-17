package com.ProjetoSD.web.Models;

/**
 * RegisterRequest
 */
public class RegisterRequest {
    /** the username */
    private String username;
    /** the password */
    private String password;
    /** the first name */
    private String firstName;
    /** the last name */
    private String lastName;
    /** empty constructor */
    public RegisterRequest() {
    }

    /**
     * Constructor
     * @param username the username
     * @param password the password
     * @param firstName the first name
     * @param lastName the last name
     */
    public RegisterRequest(String username, String password, String firstName, String lastName) {
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    /**
     * username getter
     * @return the username
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * password getter
     * @return the password
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * username setter
     * @param username the username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * password setter
     * @param password the password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * first name getter
     * @return the first name
     */
    public String getFirstName() {
    	return this.firstName;
    }
    /**
     * last name getter
     * @return the last name
     */

    public String getLastName() {
    	return this.lastName;
    }

    /**
     * first name setter
     * @param firstName the first name
     */
    public void setFirstName(String firstName) {
    	this.firstName = firstName;
    }

    /**
     * last name setter
     * @param lastName the last name
     */
    public void setLastName(String lastName) {
    	this.lastName = lastName;
    }
}
