package com.liriosbeauty.Controller;

import com.liriosbeauty.DTO.AuthRequestDTO;
import com.liriosbeauty.DTO.AuthResponseDTO;
import com.liriosbeauty.DTO.RegisterRequestDTO;
import com.liriosbeauty.Entity.User;
import com.liriosbeauty.Entity.UserRole;
import com.liriosbeauty.Repository.UserRepository;
import com.liriosbeauty.Security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body("İstifadəçi adı artıq mövcuddur!");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(UserRole.ADMIN)
                .active(true)
                .build();

        userRepository.save(user);

        String jwtToken = jwtService.generateToken(user);
        
        return ResponseEntity.ok(new AuthResponseDTO(jwtToken, user.getFullName(), user.getRole().name()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequestDTO request) {
        
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("İstifadəçi adı və ya şifrə səhvdir!");
        }

        UserDetails user = userDetailsService.loadUserByUsername(request.getUsername());
        String jwtToken = jwtService.generateToken(user);

        User userEntity = userRepository.findByUsername(request.getUsername())
                .orElseThrow();

        return ResponseEntity.ok(new AuthResponseDTO(
                jwtToken, 
                userEntity.getFullName(), 
                userEntity.getRole().name()
        ));
    }
}
