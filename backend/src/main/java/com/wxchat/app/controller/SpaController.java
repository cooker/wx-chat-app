package com.wxchat.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({"/", "/index.html"})
    public String root() {
        return "redirect:/client/";
    }

    @GetMapping({
            "/admin",
            "/admin/",
            "/admin/login",
            "/admin/albums",
            "/admin/tools/ip-portrait"
    })
    public String admin() {
        return "forward:/admin/index.html";
    }

    @GetMapping({
            "/client",
            "/client/"
    })
    public String client() {
        return "forward:/client/index.html";
    }
}
