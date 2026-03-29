package com.liriosbeauty.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Müştəri adı mütləqdir")
    @Size(min = 2, max = 100, message = "Ad 2-100 simvol arası olmalıdır")
    @Column(nullable = false)
    private String fullName;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Telefon nömrəsi düzgün formatda deyil")
    @Column(unique = true)
    private String phone;

    @Email(message = "Email formatı düzgün deyil")
    private String email;

    @Size(max = 200, message = "Ünvan 200 simvoldan çox ola bilməz")
    private String address;

    @Size(max = 500, message = "Qeyd 500 simvoldan çox ola bilməz")
    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
