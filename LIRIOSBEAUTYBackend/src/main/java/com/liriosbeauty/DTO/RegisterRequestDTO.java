package com.liriosbeauty.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequestDTO {
    
    @NotBlank(message = "İstifadəçi adı mütləqdir")
    @Size(min = 3, max = 50)
    private String username;
    
    @NotBlank(message = "Şifrə mütləqdir")
    @Size(min = 6)
    private String password;
    
    @NotBlank(message = "Ad və soyad mütləqdir")
    private String fullName;
}
