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

/**
 * SearchCotroller class
 */
@Controller
@RequestMapping("/")
public class SearchController {

    /** the RMI server interface */
    private final RMIServerInterface sv;

    /** the hacker news top stories url */
    private String hackerNewsTopStories = "https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty";

    /** constructor to inicialize the connection to the rmiServerInterface */
    @Autowired
    SearchController(RMIServerInterface rmiServerInterface) {
        this.sv = rmiServerInterface;
    }


    /**
     * This function is called on the GET request to /topsearches. When you access the top searches page
     *
     * @param m Model object
     * @return String with the name of the topsearches template to render
     */
    @GetMapping("/topsearches")
    public String showTopsearchesPage(Model m, @RequestParam(name = "admin", required = false) boolean adm) throws RemoteException {
        return "topsearches"; // Return the name of the Thymeleaf template for the register page
    }

    /**
     * This function is called on the GET request to /pointers When you access the pointers to the url page
     * if there are no pointers to the url, it will return a message saying that there are no pointers
     * @param m Model object
     * @return String with the name of the pointers template to render
     */
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

    /**
     * This function is called on the GET request to /userstories. When you access the user stories page
     *
     * @param m Model object
     * @return String with the name of the userstories template to render
     */
    @GetMapping("/userstories")
    public String showUserstoriesPage(Model m, @RequestParam(name = "user", required = false) String user, @RequestParam(name = "admin", required = false) boolean adm) throws RemoteException {

//        System.out.println("user: " + user);

        // model serve para passar variveis para templates
        ArrayList<HackerNewsItemRecord> res = hackerNewsUser(user);
//        System.out.println("res: " + res);
        if (res.isEmpty()) {
            return "userstories";

        }
        m.addAttribute("stories", res);
        return "userstories"; // Return the name of the Thymeleaf template for the register page
    }

    /**
     *In this function we get the stories of the users, then we search for the stories with the parameter we pass in the function paramters
     * with the name of the user
     * then we add all his stories to a list and return it to send to the frontend
     *
     * @param user name of the user we want to see the stories
     * @return list with the stories of the user
     */
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

    /**
     * This function is called on the GET request to /searchlinks. When you access the searchlinks page
     * If the hackernews parameter is true, it will search for the top stories of hackernews
     * if not, it will search for the links in the barrels
     *
     * @param m Model object
     * @return String with the name of the searchlinks template to render
     */
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

    /**
     * This function is already explained in the report but
     * in this function we get the top stories of hackernews and then we search for the word we want to search in the stories
     * we only get the first 30 stories because it would take too long to get all the stories
     *
     *
     * @param search this will be the word we want to search in the hackernews
     * @return hashmap with the links and the titles of the stories
     */
    public HashMap<String, ArrayList<String>> hackerNewsTopStories(String search) {
        RestTemplate restTemplate = new RestTemplate();
        List topStories = restTemplate.getForObject(hackerNewsTopStories, List.class);

//        System.out.println(topStories);

        assert topStories != null;
        HashMap<String, ArrayList<String>> hackerNewsItemRecords = new HashMap<>();

        //add the link has the key in the Hashmap and the title as the object to the hashmap.
        // if the link is already in the hashmap, append the title to the list of titles
        for (int i = 0; i < 30; i++) {
            Integer storyId = (Integer) topStories.get(i);

            String storyItemDetailsEndpoint = String.format("https://hacker-news.firebaseio.com/v0/item/%s.json?print=pretty", storyId);
//            System.out.println(storyItemDetailsEndpoint);
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
