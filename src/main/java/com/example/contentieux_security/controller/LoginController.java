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
    public String loginPage(
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String error,
            Model model) {
        
        if (logout != null) {
            model.addAttribute("message", "Vous avez été déconnecté avec succès.");
            model.addAttribute("messageType", "success");
        }
        
        if (error != null) {
            model.addAttribute("message", "Échec de l'authentification. Veuillez réessayer.");
            model.addAttribute("messageType", "error");
        }
        
        return "login";
    }
}
