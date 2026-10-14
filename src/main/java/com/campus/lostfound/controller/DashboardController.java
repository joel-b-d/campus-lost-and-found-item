package com.campus.lostfound.controller;

import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.ItemStatus;
import com.campus.lostfound.model.ItemType;
import com.campus.lostfound.model.User;
import com.campus.lostfound.repository.ItemRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

/**
 * This is the first page you see after logging in — just the list of
 * lost items. Every controller below follows this same first-three-lines
 * pattern to check who's logged in; once you understand it here, you'll
 * recognize it everywhere else in the app.
 */
@Controller
public class DashboardController {

    @Autowired
    private ItemRepository itemRepository;

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request, Model model) {

        // Step 1: who is making this request? Look in the session.
        User user = (User) request.getSession().getAttribute("loggedInUser");

        // Step 2: not logged in? Send them to the login page instead.
        if (user == null) {
            return "redirect:/login";
        }

        // Step 3: logged in — do the actual work of this page.
        model.addAttribute("user", user);
        model.addAttribute("items", itemRepository.findByTypeAndStatus(ItemType.LOST, ItemStatus.OPEN));

        return "dashboard";
    }
}
