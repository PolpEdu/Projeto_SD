package SearchEngine;

public class Message {
    private String id;
    private String content;
    public Message(String id , String content){
        this.id = id;
        this.content = "id:"+id+"|"+content;
    }
}
