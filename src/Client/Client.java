package Client;

import java.io.Serializable;

public class Client implements Serializable {
    public String username;
    public boolean admin;

    public Client(String username,boolean admin){
        this.username = username;
        this.admin = admin;
    }
}
