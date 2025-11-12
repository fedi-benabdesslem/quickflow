package com.electronica.llmprojectbackend.service;

import com.electronica.llmprojectbackend.model.Users;
import com.electronica.llmprojectbackend.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.Authenticator;

@Service
public class UserService {
    @Autowired
    private UserRepo repo ;
    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    public Users register(Users user) {
        user.setPassword(encoder.encode(user.getPassword()));
        repo.save(user);
        return user;



    }
    @Autowired
    private JWTService jwtService ;
    @Autowired
    AuthenticationManager authManger ;

    public String verify(Users user) {
        Authentication authentication = authManger.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
        if(authentication.isAuthenticated()) {
            return jwtService.generateToken(user.getUsername());
        }
        else  {
            return "fail";
        }

    }
}
