package com.ProjetoSD.web.routes;

import com.ProjetoSD.interfaces.RMIServerInterface;
import com.ProjetoSD.web.Models.HackerNewsItemRecord;
import com.ProjetoSD.web.Models.HackerNewsUserRecord;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Controller
@RequestMapping("/")
public class SearchController {

    private final RMIServerInterface sv;

    private String hackerNewsTopStories = "https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty";

    @Autowired
    SearchController(RMIServerInterface rmiServerInterface) {
        this.sv = rmiServerInterface;
    }


    @GetMapping("/topsearches")
    public String showTopsearchesPage(Model m, @RequestParam(name = "admin", required = false) boolean adm) throws RemoteException {
        return "topsearches"; // Return the name of the Thymeleaf template for the register page
    }

    @GetMapping("/pointers")
    public String showLinkpointersPage(Model m, @RequestParam(name = "url") String url) throws RemoteException {

        // model serve para passar variveis para templates
        ArrayList<String> res = this.sv.linkPointers(url);
        if(res.isEmpty()){
            res.add("No pointers found");
        }
        m.addAttribute("pointers", res);

        return "pointers"; // Return the name of the Thymeleaf template for the register page
    }

    @GetMapping("/userstories")
    public String showUserstoriesPage(Model m, @RequestParam(name = "user", required = false) String user, @RequestParam(name = "admin", required = false) boolean adm) throws RemoteException {

        System.out.println("user: " + user);

        // model serve para passar variveis para templates
        ArrayList<HackerNewsItemRecord> res = hackerNewsUser(user);
        System.out.println("res: " + res);
        if (res.isEmpty()) {
            return "userstories";

        }
        m.addAttribute("stories", res);
        return "userstories"; // Return the name of the Thymeleaf template for the register page
    }


    public ArrayList<HackerNewsItemRecord> hackerNewsUser(String user) {
        String userEndpoins = "https://hacker-news.firebaseio.com/v0/user/" + user + ".json?print=pretty";
        ArrayList<HackerNewsItemRecord> hackerNewsUserRecords = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();

        HackerNewsUserRecord hackerNewsUserRecord = restTemplate.getForObject(userEndpoins, HackerNewsUserRecord.class);

        // {"about":"","created":1175289467,"delay":0,"id":"test","karma":1,"submitted":[1043201,1029445,1026445,586568,418362,418361,11780]}

        // for each story id, get the story details https://hacker-news.firebaseio.com/v0/item/<number>.json?print=pretty
        assert hackerNewsUserRecord != null;
        assert hackerNewsUserRecord.submitted() != null;
        if (hackerNewsUserRecord == null) {
            return hackerNewsUserRecords;
        } else if (hackerNewsUserRecord.submitted() == null) {
            return hackerNewsUserRecords;
        }

        for (Object storyId : hackerNewsUserRecord.submitted()) {
            String storyItemDetailsEndpoint = String.format("https://hacker-news.firebaseio.com/v0/item/%s.json?print=pretty", storyId);
            HackerNewsItemRecord hackerNewsItemRecord = restTemplate.getForObject(storyItemDetailsEndpoint, HackerNewsItemRecord.class);

            // filter out the stories that don't have a url or title
            if (hackerNewsItemRecord == null || hackerNewsItemRecord.url() == null || hackerNewsItemRecord.title() == null) {
                continue;
            }

            hackerNewsUserRecords.add(hackerNewsItemRecord);
        }

        return hackerNewsUserRecords;
    }

    @GetMapping("/searchlinks")
    public String fetchLinks(Model m,
                             @RequestParam(name = "s") String search,
                             @RequestParam(name = "h", required = false) boolean hackernews,
                             @RequestParam(name = "page") int page)
            throws RemoteException {
        System.out.println("searching for: " + search);
        System.out.println("hackernews: " + hackernews);

        HashMap<String, ArrayList<String>> res;

        if (hackernews) {
            res = hackerNewsTopStories(search);
        } else {
            res = this.sv.searchLinks(search, page);
        }
        System.out.println(res);
        m.addAttribute("hackernewslist", res);
        // first, get the links from the search model
        return "searchlinks";
    }


    public HashMap<String, ArrayList<String>> hackerNewsTopStories(String search) {
        RestTemplate restTemplate = new RestTemplate();
        List topStories = restTemplate.getForObject(hackerNewsTopStories, List.class);

        System.out.println(topStories);

        assert topStories != null;
        HashMap<String, ArrayList<String>> hackerNewsItemRecords = new HashMap<>();

        //add the link has the key in the Hashmap and the title as the object to the hashmap.
        // if the link is already in the hashmap, append the title to the list of titles
        for (int i = 0; i < 30; i++) {
            Integer storyId = (Integer) topStories.get(i);

            String storyItemDetailsEndpoint = String.format("https://hacker-news.firebaseio.com/v0/item/%s.json?print=pretty", storyId);
            System.out.println(storyItemDetailsEndpoint);
            HackerNewsItemRecord hackerNewsItemRecord = restTemplate.getForObject(storyItemDetailsEndpoint, HackerNewsItemRecord.class);
            if (search != null) {
                List<String> searchTermsList = List.of(search.toLowerCase().split(" "));
                if (searchTermsList.stream().anyMatch(hackerNewsItemRecord.title().toLowerCase()::contains)) {
                    if (hackerNewsItemRecords.containsKey(hackerNewsItemRecord.url())) {
                        hackerNewsItemRecords.get(hackerNewsItemRecord.url()).add(hackerNewsItemRecord.title());
                    } else {
                        ArrayList<String> titles = new ArrayList<>();
                        titles.add(hackerNewsItemRecord.title());
                        hackerNewsItemRecords.put(hackerNewsItemRecord.url(), titles);
                    }
                }
            } else {
                if (hackerNewsItemRecords.containsKey(hackerNewsItemRecord.url())) {
                    hackerNewsItemRecords.get(hackerNewsItemRecord.url()).add(hackerNewsItemRecord.title());
                } else {
                    ArrayList<String> titles = new ArrayList<>();
                    titles.add(hackerNewsItemRecord.title());
                    hackerNewsItemRecords.put(hackerNewsItemRecord.url(), titles);
                }
            }
        }
        return hackerNewsItemRecords;
    }
}
