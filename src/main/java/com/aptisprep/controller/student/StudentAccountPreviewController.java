package com.aptisprep.controller.student;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** GET-only placeholders. No profile writes or placement attempts. */
@Controller
public class StudentAccountPreviewController {
    @GetMapping("/student/profile")
    public String profile(Principal principal, Model model) {
        model.addAttribute("email", principal.getName());
        return "student/account/profile";
    }
    @GetMapping("/student/placement")
    public String placement() { return "student/account/placement"; }
}
