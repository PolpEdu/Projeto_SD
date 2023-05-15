package com.ProjetoSD.web.Models;

public class FormRequest {
    private String username;
    private String password;

    public FormRequest() {
    }

    public FormRequest(String username, String password) {
        this.username = username;
        this.password = password;
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
}
