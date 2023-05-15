package com.ProjetoSD.web.routes;

import com.ProjetoSD.Client.Client;
import com.ProjetoSD.interfaces.RMIServerInterface;
import com.ProjetoSD.web.HackerNewsItemRecord;
import com.ProjetoSD.web.HackerNewsUserRecord;
import com.ProjetoSD.web.Models.FormRequest;
import com.ProjetoSD.web.Models.IndexRequest;
import com.ProjetoSD.web.Models.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

@Controller
@RequestMapping("/")
class AuthController {
    private final RMIServerInterface sv;

    @Autowired
    AuthController(RMIServerInterface rmiServerInterface) {
        this.sv = rmiServerInterface;
    }

    @GetMapping("/")
    public String showIndexPage(Model m) {
        // model serve para passar variveis para templates
        m.addAttribute("IndexRequest", new IndexRequest());
        return "index"; // Return the name of the Thymeleaf template for the index page
    }

    @GetMapping("/login")
    public String showLoginPage(Model m) {
        // model serve para passar variveis para templates

        // crio um objeto do tipo FormRequest que vai ser preenchido pelo formulario
        m.addAttribute("FormRequest", new FormRequest());

        return "login"; // Return the name of the Thymeleaf template for the login page
    }

    @PostMapping("/login")
    public String handleLoginFormSubmission(@ModelAttribute FormRequest fr) throws RemoteException {
        // Process the email and password data
        // Perform authentication and validation logic here
        // Redirect to the appropriate page based on the login result

        String email = fr.getUsername();
        String password = fr.getPassword();

        ArrayList<String> checked = this.sv.checkLogin(email, password);
        System.out.println(checked);
        if (checked.get(0).equals("true")) {
            boolean admin = checked.get(1).equals("true");
            Client client = new Client(email, admin);

            if (admin) {
                System.out.println("[CLIENT] Login successful as admin");
            } else {
                System.out.println("[CLIENT] Login successful");
            }

            // Redirect to the dashboard page with admin flag set
            return "redirect:/dashboard?admin=" + admin;
        }
        System.out.println("[CLIENT] Login failed");

        return "redirect:/login?error=true"; // Redirect back to the login page with an error parameter
    }


    @GetMapping("/register")
    public String showRegisterPage(Model m) {
        // model serve para passar variveis para templates
        m.addAttribute("RegisterRequest", new RegisterRequest());
        return "register"; // Return the name of the Thymeleaf template for the register page
    }

    @PostMapping("/register")
    public String handleRegisterFormSubmission(@ModelAttribute RegisterRequest rr) throws RemoteException {
        // Process the email and password data
        // Perform authentication and validation logic here
        // Redirect to the appropriate page based on the login result

        String username = rr.getUsername();
        String password = rr.getPassword();
        String firstName = rr.getFirstName();
        String lastName = rr.getLastName();

        ArrayList<String> res = this.sv.checkRegister(username, password, firstName, lastName);

        boolean registered = res.get(0).equals("true");
        if (registered) {
            System.out.println("[CLIENT] Registration successful");
            Client c = new Client(username, res.get(1).equals("true"));

            return "redirect:/login?username=" + username + "&registered=true";
        }
        System.out.println("[CLIENT] Registration failed");

        return "redirect:/register?error=true"; // Redirect back to the register page with an error parameter
    }


    /**
     * This function is called on the POST request to /indexnewurl. When you press the button to index a new url.
     * @param ir IndexRequest object with the url to index
     * @return String with the name of the template to render
     * @throws RemoteException if there is an error with the RMI connection
     */
    @PostMapping("/indexnewurl")
    public String handleIndexNewUrlFormSubmission(@ModelAttribute IndexRequest ir) throws RemoteException {
        String url = ir.getUrl();

        // System.out.println("[CLIENT] IndexNewUrl requested for url: " + url);

        if (this.sv.indexNewUrl(url)) {
            System.out.println("[CLIENT] IndexNewUrl successful");
            return "redirect:/?success=true";
        }
        return "redirect:/?error=true" ; // Redirect back to the login page with an error parameter
    }

    @GetMapping("/dashboard")
    public String showdashboard(Model m, @RequestParam(name = "admin", required = false) boolean adm) {
        m.addAttribute("IndexRequest", new IndexRequest());


        // check if admin is null, if so, set it to false
        if (adm) {
            System.out.println("[CLIENT] Dashboard page requested as admin");
        } else {
            System.out.println("[CLIENT] Dashboard page requested");
        }

        // model serve para passar variveis para templates
        m.addAttribute("admin", adm);
        return "dashboard"; // Return the name of the Thymeleaf template for the dashboard page
    }

    //get top stories
    @GetMapping("/topStories")
    @ResponseBody
    public List<HackerNewsItemRecord> hackerNewsTopStories(@RequestParam(name = "search", required = false) String search){
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
        return hackerNewsItemRecords;
    }

    //get users
    @GetMapping("hackerNewsUsers")
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
    public String fetchLinks(Model m , @RequestParam(name = "s", required = true) String search,  @RequestParam(name = "h", required = false) boolean hackernews) throws RemoteException {
        if (hackernews) {
            // get request here
        }
        // first, get the links from the search model
        HashMap<String, ArrayList<String>> res = this.sv.searchLinks(search);

        m.addAttribute("linksfound", res);



        return "redirect:/searchlinks?s=" + search;
    }
}
