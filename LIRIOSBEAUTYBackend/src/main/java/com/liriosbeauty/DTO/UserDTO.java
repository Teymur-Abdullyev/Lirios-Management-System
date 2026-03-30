package com.liriosbeauty.DTO;

import com.liriosbeauty.Entity.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserDTO {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private UserRole role;
    private Boolean active;
    private LocalDateTime createdAt;
    private Long employeeId;
}
