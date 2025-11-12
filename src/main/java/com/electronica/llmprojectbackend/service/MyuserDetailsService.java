package com.electronica.llmprojectbackend.service;

import com.electronica.llmprojectbackend.model.UserPrincipal;
import com.electronica.llmprojectbackend.model.Users;
import com.electronica.llmprojectbackend.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyuserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepo repo ;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException{
        Users user = repo.findByUsername(username);
        if(user==null){
            throw new UsernameNotFoundException("username is null");
        }
        return new UserPrincipal(user);
    };

}
