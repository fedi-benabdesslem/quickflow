package com.electronica.llmprojectbackend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*") // Configure this properly for production
@Slf4j
public class HomeController {
    @GetMapping("/")
    public String greet() {
        return "Hello World!";
    }
}
