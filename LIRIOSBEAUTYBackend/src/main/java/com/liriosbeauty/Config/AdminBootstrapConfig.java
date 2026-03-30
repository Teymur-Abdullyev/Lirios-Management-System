package com.liriosbeauty.Config;

import com.liriosbeauty.Entity.User;
import com.liriosbeauty.Entity.UserRole;
import com.liriosbeauty.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapConfig implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

        if (changed) {
            userRepository.save(adminUser);
            log.info("Bootstrap admin normalized: {}", adminUsername);
        }
    }
}
