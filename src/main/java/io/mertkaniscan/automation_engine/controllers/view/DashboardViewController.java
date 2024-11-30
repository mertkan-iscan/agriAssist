package io.mertkaniscan.automation_engine.controllers.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardViewController {

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("message", "Welcome to the Dashboard!");

        return "dashboard";
    }
}