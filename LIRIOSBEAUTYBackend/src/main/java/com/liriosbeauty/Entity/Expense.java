package com.liriosbeauty.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Data
@NoArgsConstructor
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Xərc təsviri mütləqdir")
    @Size(min = 3, max = 200, message = "Təsvir 3-200 simvol arası olmalıdır")
    @Column(nullable = false)
    private String description;

    @NotNull(message = "Məbləğ mütləqdir")
    @DecimalMin(value = "0.01", message = "Məbləğ 0-dan böyük olmalıdır")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Kateqoriya mütləqdir")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Size(max = 500, message = "Qeyd 500 simvoldan çox ola bilməz")
    private String notes;
}
