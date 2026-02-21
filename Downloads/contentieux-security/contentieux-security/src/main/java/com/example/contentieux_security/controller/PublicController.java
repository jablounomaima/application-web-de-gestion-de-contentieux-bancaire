package com.example.contentieux_security.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicController {

    @GetMapping("/public/hello")
    public String publicHello() {
        return "Hello public!";
    }
}