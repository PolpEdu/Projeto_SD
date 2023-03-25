package SearchEngine;

public class Message {
    private int id;
    private String content;
    public Message(int id , String content){
        this.id = id;
        this.content = "id:"+id+"|"+content;
    }
}
