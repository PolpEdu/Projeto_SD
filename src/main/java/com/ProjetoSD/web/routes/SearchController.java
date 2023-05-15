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

    @Autowired
    SearchController(RMIServerInterface rmiServerInterface) {
        this.sv = rmiServerInterface;
    }


    @GetMapping("/topsearches")
    public String showTopsearchesPage(Model m, @RequestParam(name = "admin", required = false) boolean adm) throws RemoteException {

        // model serve para passar variveis para templates

        m.addAttribute("searches", this.sv.getTop10Searches());
        m.addAttribute("admin", adm);
        return "topsearches"; // Return the name of the Thymeleaf template for the register page
    }


    //get users
    @GetMapping("/hackerNewsUsers")
    @ResponseBody
    public List<HackerNewsUserRecord> hackerNewsUsers() {
        List<String> userEndpoins = List.of("https://hacker-news.firebaseio.com/v0/user/jl.json?print=pretty");
        List<HackerNewsUserRecord> hackerNewsUserRecords = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();

        for (String endpoint : userEndpoins) {
            HackerNewsUserRecord hackerNewsUserRecord = restTemplate.getForObject(endpoint, HackerNewsUserRecord.class);
            hackerNewsUserRecords.add(hackerNewsUserRecord);
            System.out.println(hackerNewsUserRecord);
        }
        return hackerNewsUserRecords;
    }

    @GetMapping("/searchLinks")
    public String fetchLinks(Model m , @RequestParam(name = "s", required = true) String search, @RequestParam(name = "h", required = false) boolean hackernews) throws RemoteException {
        HashMap<String, ArrayList<String>> res = new HashMap<>();

        if (hackernews) {
            ArrayList<HackerNewsItemRecord> hackernewslist = new ArrayList<>();
            //res.put("res", hackernewslist);

        }
        else{
            res = this.sv.searchLinks(search);
        }


        // first, get the links from the search model



        m.addAttribute("linksfound", res);

        return "redirect:/searchlinks?s=" + search;
    }

    @GetMapping("/topStories")
    @ResponseBody
    public HashMap<String, ArrayList<String>> hackerNewsTopStories(@RequestParam(name = "search", required = false) String search){
        String endPoint = "https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty";
        RestTemplate restTemplate = new RestTemplate();

        List topStories  = restTemplate.getForObject(endPoint, List.class);

        assert topStories != null;
        List<HackerNewsItemRecord> hackerNewsItemRecords = new ArrayList<>();

        //iterar apenas por 30
        for (int i = 0; i <= 30;i++){
            Integer storyId = (Integer) topStories.get(i);

            String storyItemDetailsEndpoint = String.format("https://hacker-news.firebaseio.com/v0/item/%s.json?print=pretty", storyId);
            HackerNewsItemRecord hackerNewsItemRecord = restTemplate.getForObject(storyItemDetailsEndpoint, HackerNewsItemRecord.class);

            if (search != null) {
                List<String> searchTermsList = List.of(search.toLowerCase().split(" "));
                if (searchTermsList.stream().anyMatch(hackerNewsItemRecord.title().toLowerCase()::contains))

                    hackerNewsItemRecords.add(hackerNewsItemRecord);

            } else {
                System.out.println("No search terms");
                hackerNewsItemRecords.add(hackerNewsItemRecord);
            }
        }
        return null;
    }
}
