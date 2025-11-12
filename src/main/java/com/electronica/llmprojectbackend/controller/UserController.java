package com.electronica.llmprojectbackend.controller;

import com.electronica.llmprojectbackend.model.Users;
import com.electronica.llmprojectbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    @Autowired
    private UserService service ;
    @PostMapping("/register")
    public Users Register(@RequestBody Users user){
        return service.register(user) ;

    }
    @PostMapping("/login")
    public String login(@RequestBody Users user){
        return service.verify(user) ;

    }
}
