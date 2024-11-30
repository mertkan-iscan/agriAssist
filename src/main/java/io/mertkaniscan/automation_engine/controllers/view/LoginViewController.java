package io.mertkaniscan.automation_engine.controllers.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginViewController {
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }
}