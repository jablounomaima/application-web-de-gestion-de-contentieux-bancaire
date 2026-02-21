package com.example.contentieux_security.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrivateController {

    @GetMapping("/private/hello")
    public String privateHello() {
        return "Hello secured!";
    }
}