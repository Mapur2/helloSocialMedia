package com.userservice.controller;

import com.userservice.dto.*;
import com.userservice.entity.User;
import com.userservice.service.AuthService;
import com.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder encoder;

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getUserProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UserResponse r = userService.mapToUserResponse(userService.getUserByEmailId(email));
        return ResponseEntity.ok(r);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            request.setPassword(encoder.encode(request.getPassword()));
            return ResponseEntity.ok(userService.register(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{userId}/validate")
    public ResponseEntity<Boolean> validateUser(@PathVariable String userId) {
        return ResponseEntity.ok(userService.exitsByUserId(userId));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> signin(@RequestBody LoginDTO loginDTO) {
        LoginResponse login = authService.login(loginDTO);
        return new ResponseEntity<>(login, HttpStatus.OK);
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestParam String token) {
        return ResponseEntity.ok(authService.validateToken(token));
    }

    @PostMapping("/followers")
    public UserFollowerList getFollowers(@RequestBody UserIds user) {
        for (String s : user.getIds()) {
            System.out.println(s);
        }
        return userService.getUsers(user.getIds());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<Map<String, Object>> isUsernameAvailable(@PathVariable String username) {
        User user = userService.getUserByUsername(username);

        if (user == null) {
            return new ResponseEntity<>(
                    Map.of("available", true, "user", "null"),
                    HttpStatus.OK);
        }

        user.setPassword(null);
        Map<String, Object> response = new HashMap<>();  // ← HashMap allows null values
        response.put("available", false);
        response.put("data", user);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/usernames")
    public Usernames getUsernames(@RequestBody UserIds userIds) {
        return userService.getUsernames(userIds.getIds());
    }
}
