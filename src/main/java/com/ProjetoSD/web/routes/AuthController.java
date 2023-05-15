package com.ProjetoSD.web.routes;

import com.ProjetoSD.Client.Client;
import com.ProjetoSD.interfaces.RMIServerInterface;
import com.ProjetoSD.web.Models.FormRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.rmi.RemoteException;
import java.util.ArrayList;

@Controller
@RequestMapping("/")
class AuthController {
    private final RMIServerInterface sv;

    @Autowired
    AuthController(RMIServerInterface rmiServerInterface) {
        this.sv = rmiServerInterface;
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

    @PostMapping("/register")
    public String handleRegisterFormSubmission(
            @RequestParam("email") String username,
            @RequestParam("password") String password,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName
    ) throws RemoteException {
        // Process the email and password data
        // Perform authentication and validation logic here
        // Redirect to the appropriate page based on the login result

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


    @GetMapping("/register")
    public String showRegisterPage() {
        return "register"; // Return the name of the Thymeleaf template for the register page
    }
}
