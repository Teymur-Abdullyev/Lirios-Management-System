package com.liriosbeauty.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO {
    private Long id;
    private String fullName;
    private String phone;
    private LocalDateTime registeredAt;
    private BigDecimal currentDebt;
}