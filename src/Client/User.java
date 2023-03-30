package Client;

import java.io.Serializable;
import java.util.ArrayList;

public class User implements Serializable {
    public String username;
    public String password;

    public String name;
    public String surname;

    public boolean admin;
    public boolean notify;

    public ArrayList<String> searchHistory;

    public User(String username, String password, boolean admin, String name, String surname) {
        this.username = username;
        this.password = password;
        this.admin = admin;
        this.searchHistory = new ArrayList<>();
        this.name = name;
        this.surname = surname;
        this.notify = false;
    }
}
