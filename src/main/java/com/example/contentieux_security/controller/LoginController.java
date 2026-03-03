import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;  // <-- pour @RequestParam
import org.springframework.ui.Model;  // <-- pour Model

@Controller
public class LoginController {



    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/home")
    public String home() {
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login"; // login.html
    }

    
}
