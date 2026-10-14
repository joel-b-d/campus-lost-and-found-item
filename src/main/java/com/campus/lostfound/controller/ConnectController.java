package com.campus.lostfound.controller;

import com.campus.lostfound.service.ConnectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The "connect other devices" page. It needs no login on purpose: someone
 * holding a phone should be able to open it before they have an account.
 * It only shows this laptop's network addresses, nothing private.
 */
@Controller
public class ConnectController {

    @Autowired
    private ConnectService connectService;

    @GetMapping("/connect")
    public String connect(Model model) {
        model.addAttribute("entries", connectService.findEntries());
        model.addAttribute("port", connectService.getPort());
        return "connect";
    }
}
