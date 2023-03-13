package Utility;

public class Message {

    public String message;
    public String id;

    public Message(String message, String id) {
        this.message = "[" + id + "] " + message;
        this.id = id;
    }
}