package com.example.contentieux_security.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/public/hello")
    public String publicHello() {
        return "Hello public!";
    }

    @GetMapping("/private/hello")
    public String privateHello() {
        return "Hello secured!";
    }

    @GetMapping("/admin/hello")
    public String adminHello() {
        return "Hello admin!";
    }
}