package com.ai.application.Config;

import com.ai.application.model.Entity.User;
import com.ai.application.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Autowired
    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Create default admin account if it doesn't exist
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User("admin", "admin@example.com", "admin");
            userRepository.save(admin);
            System.out.println("Default admin account created: username=admin, password=admin");
        }
    }
}

