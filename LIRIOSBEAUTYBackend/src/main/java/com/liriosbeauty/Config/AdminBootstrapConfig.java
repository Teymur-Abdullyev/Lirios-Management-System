package com.liriosbeauty.Config;

import com.liriosbeauty.Entity.User;
import com.liriosbeauty.Entity.UserRole;
import com.liriosbeauty.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapConfig implements CommandLineRunner {

    private static final String DEFAULT_ADMIN_USERNAME = "admin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BCryptPasswordEncoder bcryptPasswordEncoder = new BCryptPasswordEncoder();

    @Value("${app.bootstrap-admin.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${app.bootstrap-admin.username:admin}")
    private String adminUsername;

    @Value("${app.bootstrap-admin.password:admin123}")
    private String adminPassword;

    @Value("${app.bootstrap-admin.full-name:System Admin}")
    private String adminFullName;

    @Value("${app.bootstrap-admin.email:admin@lirios.local}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        if (!bootstrapEnabled) {
            return;
        }

        if (!DEFAULT_ADMIN_USERNAME.equalsIgnoreCase(adminUsername)) {
            userRepository.findByUsername(DEFAULT_ADMIN_USERNAME).ifPresent(defaultAdmin -> {
                if (defaultAdmin.getCreatedAt() == null) {
                    defaultAdmin.setCreatedAt(LocalDateTime.now());
                }
                if (defaultAdmin.isActive()) {
                    defaultAdmin.setActive(false);
                    userRepository.save(defaultAdmin);
                    log.info("Default admin account disabled: {}", DEFAULT_ADMIN_USERNAME);
                }
            });
        }

        User adminUser = userRepository.findByUsername(adminUsername).orElse(null);
        if (adminUser == null) {
            User created = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .fullName(adminFullName)
                    .email(adminEmail)
                    .role(UserRole.ADMIN)
                    .active(true)
                    .build();
            userRepository.save(created);
            log.info("Bootstrap admin created: {}", adminUsername);
            return;
        }

        boolean changed = false;
        if (adminUser.getRole() != UserRole.ADMIN) {
            adminUser.setRole(UserRole.ADMIN);
            changed = true;
        }
        if (!adminUser.isActive()) {
            adminUser.setActive(true);
            changed = true;
        }

        if (!userRepository.existsByRole(UserRole.ADMIN)) {
            adminUser.setRole(UserRole.ADMIN);
            changed = true;
        }

        // Keep known bootstrap credentials for first login after deployment.
        if (!bcryptPasswordEncoder.matches(adminPassword, adminUser.getPassword())) {
            adminUser.setPassword(passwordEncoder.encode(adminPassword));
            changed = true;
        }

        if (adminUser.getCreatedAt() == null) {
            adminUser.setCreatedAt(LocalDateTime.now());
            changed = true;
        }

        if (changed) {
            userRepository.save(adminUser);
            log.info("Bootstrap admin normalized: {}", adminUsername);
        }
    }
}
