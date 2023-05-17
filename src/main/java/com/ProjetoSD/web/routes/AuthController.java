package com.ProjetoSD.web.routes;

import com.ProjetoSD.Client.Client;
import com.ProjetoSD.interfaces.RMIServerInterface;
import com.ProjetoSD.web.Models.FormRequest;
import com.ProjetoSD.web.Models.IndexRequest;
import com.ProjetoSD.web.Models.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * AuthController class
 */
@Controller
@RequestMapping("/")
class AuthController {
    /** the RMI server interface */
    private final RMIServerInterface sv;

    /**
     * Constructor
     * @param rmiServerInterface the RMI server interface
     */
    @Autowired
    AuthController(RMIServerInterface rmiServerInterface) {
        this.sv = rmiServerInterface;
    }

    /**
     * This function is called on the GET request to /index. When you access the index page.
     * @param m Model object
     * @return String with the name of the index template to render
     */
    @GetMapping("/")
    public String showIndexPage(Model m) {
        // model serve para passar variveis para templates
        m.addAttribute("IndexRequest", new IndexRequest());
        return "index"; // Return the name of the Thymeleaf template for the index page
    }

    /**
     * This function is called on the GET request to /login. When you access the login page
     *
     * @param m Model object
     * @return String with the name of the login template to render
     */
    @GetMapping("/login")
    public String showLoginPage(Model m) {
        // model serve para passar variveis para templates

        // crio um objeto do tipo FormRequest que vai ser preenchido pelo formulario
        m.addAttribute("FormRequest", new FormRequest());

        return "login"; // Return the name of the Thymeleaf template for the login page
    }

    /**
     * This function is called on the POST request to /login. When you submit the login form
     * In this function you should process the login form data and perform the authentication and validation logic
     *
     * @param fr FormRequest object
     * @return String with the name of the dashboard template to render or the login template with an error parameter
     */
    @PostMapping("/login")
    public String handleLoginFormSubmission(@ModelAttribute FormRequest fr) throws RemoteException {
        // Process the email and password data
        // Perform authentication and validation logic here
        // Redirect to the appropriate page based on the login result

        String email = fr.getUsername();
        String password = fr.getPassword();

        ArrayList<String> checked = this.sv.checkLogin(email, password);
        System.out.println(checked);
        if (checked == null || checked.isEmpty()) {
            System.out.println("[CLIENT] Login failed");
            return "redirect:/login?error=true"; // Redirect back to the login page with an error parameter
        }
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


    /**
     * This function is called on the GET request to /register. When you access the register page
     *
     * @param m Model object
     * @return String with the name of the register template to render
     */
    @GetMapping("/register")
    public String showRegisterPage(Model m) {
        // model serve para passar variveis para templates
        m.addAttribute("RegisterRequest", new RegisterRequest());
        return "register"; // Return the name of the Thymeleaf template for the register page
    }

    /**
     * This function is called on the POST request to /register. When you submit the register form
     * In this function you should process the register form data and perform the validation logic
     *
     * @param rr RegisterRequest object
     * @return String with the name of the login template to render or the register template with an error parameter
     */
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

    /**
     * This function is called on the GET request to /dashboard. When you access the dashboard page
     * @param m Model object
     * @param adm boolean to check if the user is admin
     * @return String with the name of the dashboard template to render
     */
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
}
