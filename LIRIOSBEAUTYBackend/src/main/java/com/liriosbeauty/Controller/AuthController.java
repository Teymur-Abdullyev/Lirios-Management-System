package com.liriosbeauty.Controller;

import com.liriosbeauty.DTO.*;
import com.liriosbeauty.Entity.User;
import com.liriosbeauty.Repository.UserRepository;
import com.liriosbeauty.Service.UserService;
import com.liriosbeauty.Security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final UserService userService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return ResponseEntity.ok(userService.toDto(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid username or password");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return ResponseEntity.ok(LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                .user(userService.toDto(user))
                .build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        String username;
        try {
            username = jwtService.extractUsername(refreshToken);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid refresh token");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtService.isTokenValid(refreshToken, userDetails, "refresh")) {
            return ResponseEntity.badRequest().body("Refresh token is expired or invalid");
        }

        User user = userRepository.findByUsername(username).orElseThrow();
        String accessToken = jwtService.generateAccessToken(userDetails);

        return ResponseEntity.ok(LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                .user(userService.toDto(user))
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(userService.toDto(user));
    }
}
