package com.marketplace.controller;

import com.marketplace.dto.AuthRegisterRequest;
import com.marketplace.dto.AuthRequest;
import com.marketplace.dto.AuthResponse;
import com.marketplace.model.LoginHistory;
import com.marketplace.model.Role;
import com.marketplace.model.Seller;
import com.marketplace.model.User;
import com.marketplace.repository.LoginHistoryRepository;
import com.marketplace.repository.RoleRepository;
import com.marketplace.repository.SellerRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SellerRepository sellerRepository;
    private final PasswordEncoder encoder;

    private final LoginHistoryRepository loginHistoryRepository;

    public AuthController(AuthenticationManager authManager, JwtService jwtService, UserRepository userRepository, RoleRepository roleRepository, SellerRepository sellerRepository, PasswordEncoder encoder, LoginHistoryRepository loginHistoryRepository) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.sellerRepository = sellerRepository;
        this.encoder = encoder;
        this.loginHistoryRepository = loginHistoryRepository;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@RequestBody AuthRegisterRequest req) {

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        boolean isSeller = Boolean.TRUE.equals(req.getIsSeller());
        String roleName = isSeller ? "SELLER" : "BUYER";

        Role userRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Role " + roleName + " not found"
                ));

        User user = new User();
        user.setEmail(req.getEmail());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setCreatedAt(Instant.now());
        user.getRoles().add(userRole);

        user = userRepository.save(user);

        if (isSeller) {
            Seller seller = new Seller();
            seller.setUser(user);

            String storeName = "Магазин " + user.getEmail().split("@")[0];
            seller.setStoreName(storeName);
            seller.setRating(BigDecimal.ZERO);

            sellerRepository.save(seller);
        }
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest req, HttpServletRequest httpReq) {

        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        String token = jwtService.generateToken(req.getEmail(), auth.getAuthorities());

        // журнализация входа
        User u = userRepository.findByEmail(req.getEmail()).orElseThrow();
        LoginHistory lh = new LoginHistory();
        lh.setUser(u);
        lh.setLoginTime(Instant.now());
        lh.setIpAddress(httpReq.getRemoteAddr());
        loginHistoryRepository.save(lh);

        return new AuthResponse(token);
    }
}