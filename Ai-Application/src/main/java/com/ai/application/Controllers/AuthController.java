package com.ai.application.Controllers;
import com.ai.application.model.Entity.User;
import com.ai.application.Repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "user", userOpt.get()
            ));
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

        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "exist",
                    "message", "User already exists"
            ));
        }

        User user = new User(username, email, password);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Account created!"
        ));
    }
}
