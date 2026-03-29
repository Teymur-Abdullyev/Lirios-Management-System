package com.liriosbeauty.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequestDTO {
    
    @NotBlank(message = "İstifadəçi adı mütləqdir")
    @Size(min = 3, max = 50, message = "İstifadəçi adı 3-50 simvol arası olmalıdır")
    private String username;
    
    @NotBlank(message = "Şifrə mütləqdir")
    @Size(min = 6, message = "Şifrə minimum 6 simvol olmalıdır")
    private String password;
}
