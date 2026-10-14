package com.campus.lostfound.controller;

import com.campus.lostfound.model.User;
import com.campus.lostfound.repository.NotificationRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping("/notifications")
    public String notificationsPage(HttpServletRequest request, Model model) {
        User user = (User) request.getSession().getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        model.addAttribute("notifications", notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
        return "notifications";
    }
}
