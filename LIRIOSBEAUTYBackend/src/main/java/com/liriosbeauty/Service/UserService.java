package com.liriosbeauty.Service;

import com.liriosbeauty.DTO.RegisterRequest;
import com.liriosbeauty.DTO.UserDTO;
import com.liriosbeauty.Entity.Employee;
import com.liriosbeauty.Entity.User;
import com.liriosbeauty.Repository.EmployeeRepository;
import com.liriosbeauty.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Employee employee = null;
        if (request.getEmployeeId() != null) {
            employee = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Employee not found: " + request.getEmployeeId()));
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .role(request.getRole())
                .active(true)
                .employee(employee)
                .build();

        return userRepository.save(user);
    }

    public UserDTO toDto(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .employeeId(user.getEmployee() != null ? user.getEmployee().getId() : null)
                .build();
    }
}
