package com.campus.lostfound.controller;

import com.campus.lostfound.model.Role;
import com.campus.lostfound.model.User;
import com.campus.lostfound.repository.UserRepository;
import com.campus.lostfound.util.PasswordUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** Admin-only: view every account, disable one, or create a Staff/Admin account. */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/users")
    public String usersPage(HttpServletRequest request, Model model) {
        User user = (User) request.getSession().getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        if (user.getRole() != Role.ADMIN) return "redirect:/dashboard"; // only Admin controls the app

        model.addAttribute("user", user);
        model.addAttribute("users", userRepository.findAll());
        return "admin-users";
    }

    @PostMapping("/users/{id}/disable")
    public String disableUser(@PathVariable Long id, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        if (user.getRole() != Role.ADMIN) return "redirect:/dashboard";

        User target = userRepository.findById(id).orElseThrow();
        target.setEnabled(false);
        userRepository.save(target);

        return "redirect:/admin/users";
    }

    @PostMapping("/users/new")
    public String createUser(@RequestParam String fullName,
                              @RequestParam String email,
                              @RequestParam String password,
                              @RequestParam Role role,
                              HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        if (user.getRole() != Role.ADMIN) return "redirect:/dashboard";

        User newUser = new User();
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setPassword(PasswordUtil.hash(password));
        newUser.setRole(role);
        userRepository.save(newUser);

        return "redirect:/admin/users";
    }
}
