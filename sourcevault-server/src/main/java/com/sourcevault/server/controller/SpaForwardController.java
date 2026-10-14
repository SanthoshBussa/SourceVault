package com.sourcevault.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping(value = {
            "/",
            "/analysis",
            "/analysis/**",
            "/collection",
            "/collection/**",
            "/results",
            "/results/**",
            "/history",
            "/history/**",
            "/settings",
            "/settings/**"
    })
    public String forwardSpa() {
        return "forward:/index.html";
    }
}
