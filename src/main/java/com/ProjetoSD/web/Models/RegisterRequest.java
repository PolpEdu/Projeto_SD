package com.ProjetoSD.web.Models;

public class RegisterRequest {
    private String username;
    private String password;

    private String firstName;

    private String lastName;

    public RegisterRequest() {
    }

    public RegisterRequest(String username, String password, String firstName, String lastName) {
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // Getters and setters omitted for brevity
    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
    	return this.firstName;
    }

    public String getLastName() {
    	return this.lastName;
    }

    public void setFirstName(String firstName) {
    	this.firstName = firstName;
    }

    public void setLastName(String lastName) {
    	this.lastName = lastName;
    }
}
