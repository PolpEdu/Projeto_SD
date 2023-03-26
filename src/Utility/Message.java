package Utility;

public class Message {

    public String message;
    public String id;

    public Message(String id,String message ) {
        this.message = "id:"+id + "|" + message;
        this.id = id;
    }

    public String getType() {
        String[] parts = this.message.split("\\|");
        return parts[0];
    }
}