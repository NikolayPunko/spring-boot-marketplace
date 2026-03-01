package com.marketplace.controller;

import com.marketplace.model.Role;
import com.marketplace.model.User;
import com.marketplace.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MeController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public MeResponse me(Authentication auth) {

        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        User u = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        List<String> roles = u.getRoles().stream()
                .map(Role::getName)
                .toList();

        return new MeResponse(u.getId(), u.getEmail(), roles);
    }

    @Data
    @AllArgsConstructor
    public static class MeResponse {
        private Long id;
        private String email;
        private List<String> roles;
    }
}