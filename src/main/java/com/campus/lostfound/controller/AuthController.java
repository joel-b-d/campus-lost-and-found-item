package com.campus.lostfound.controller;

import com.campus.lostfound.model.Role;
import com.campus.lostfound.model.User;
import com.campus.lostfound.repository.UserRepository;
import com.campus.lostfound.util.PasswordUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Optional;

/**
 * Handles login, logout, and student sign-up.
 *
 * There is no Spring Security here. "Being logged in" just means:
 * we put the User object into the browser's session, which Spring
 * keeps alive (via a cookie) between page visits. Every other
 * controller in this app checks for that session value directly —
 * see the top of DashboardController for an example.
 */
@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/login")
    public String loginPage() {
        return "login"; // shows templates/login.html
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email,
                           @RequestParam String password,
                           HttpServletRequest request,
                           Model model) {

        Optional<User> found = userRepository.findByEmail(email);

        // Check 1: does an account with this email exist?
        // Check 2: does the typed password's hash match the stored hash?
        if (found.isEmpty() || !PasswordUtil.matches(password, found.get().getPassword())) {
            model.addAttribute("error", "Incorrect email or password.");
            return "login";
        }

        User user = found.get();
        if (!user.isEnabled()) {
            model.addAttribute("error", "This account has been disabled. Contact an admin.");
            return "login";
        }

        // Login successful — remember this user for future requests.
        HttpSession session = request.getSession();
        session.setAttribute("loggedInUser", user);

        return "redirect:/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        request.getSession().invalidate(); // forget the logged-in user
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String fullName,
                              @RequestParam String email,
                              @RequestParam String password,
                              Model model) {

        if (userRepository.findByEmail(email).isPresent()) {
            model.addAttribute("error", "An account with this email already exists.");
            return "register";
        }

        // Public sign-up always creates a STUDENT account.
        // Staff/Admin accounts are created by an admin (see AdminController).
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(PasswordUtil.hash(password));
        user.setRole(Role.STUDENT);
        userRepository.save(user);

        return "redirect:/login";
    }
}
