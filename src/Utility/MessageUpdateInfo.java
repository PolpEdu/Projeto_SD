package Utility;

import Client.User;

import java.util.ArrayList;
import java.util.HashMap;

public class MessageUpdateInfo {
    HashMap<String, ArrayList<String>> links;
    HashMap<String, ArrayList<String>> linksInfo;

    HashMap<String, ArrayList<String>> words;


    HashMap<String, User> users;
    HashMap<String, Integer> wordsCount;

    public int PORT;

    public MessageUpdateInfo(HashMap<String, ArrayList<String>> links, HashMap<String, ArrayList<String>> words, HashMap<String, ArrayList<String>> linksInfo, HashMap<String, User> users, HashMap<String, Integer> wordsCount, int PORT) {
        this.links = links;
        this.words = words;
        this.linksInfo = linksInfo;
        this.users = users;
        this.wordsCount = wordsCount;
        this.PORT = PORT;
    }

    public HashMap<String, ArrayList<String>> getLinks() { return links; }

    public HashMap<String, ArrayList<String>> getWords() {
        return words;
    }

    public HashMap<String, User> getUsers() {
        return users;
    }

    public HashMap<String, Integer> getWordsCount() {
        return wordsCount;
    }

    public int getPORT() {
        return PORT;
    }

    public HashMap<String, ArrayList<String>> getLinksInfo() { return linksInfo; }
}
