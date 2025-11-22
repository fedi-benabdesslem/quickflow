package com.ai.application.Controllers;
import com.ai.application.model.Entity.User;
import com.ai.application.Repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        // Validate input
        if (username == null || username.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Username and password are required"
            ));
        }

        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password.trim())) {
            User user = userOpt.get();
            // Create a simple token (using user ID as token for simplicity)
            String token = user.getId() != null ? user.getId() : "token_" + user.getUsername();
            
            // Return user data without password
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId() != null ? user.getId() : "");
            userData.put("username", user.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("token", token);
            response.put("user", userData);
            
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "message", "Invalid credentials"
            ));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");

        // Validate input
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Username is required"
            ));
        }
        
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Email is required"
            ));
        }
        
        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Password is required"
            ));
        }

        // Check if user already exists
        if (userRepository.existsByUsername(username.trim())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Username already exists"
            ));
        }
        
        if (userRepository.existsByEmail(email.trim())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Email already exists"
            ));
        }

        try {
            User user = new User(username.trim(), email.trim(), password.trim());
            userRepository.save(user);
            
            // Create a simple token (using user ID as token for simplicity)
            String token = user.getId() != null ? user.getId() : "token_" + user.getUsername();
            
            // Return user data without password
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId() != null ? user.getId() : "");
            userData.put("username", user.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Account created!");
            response.put("token", token);
            response.put("user", userData);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to create account: " + e.getMessage()
            ));
        }
    }
}
