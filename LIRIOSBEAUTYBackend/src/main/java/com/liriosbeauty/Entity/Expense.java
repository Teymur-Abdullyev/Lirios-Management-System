package com.liriosbeauty.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @NotNull
    @Column(name = "exchange_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal exchangeRate = new BigDecimal("1.0000");

    @NotNull
    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate = LocalDate.now();

    @NotBlank
    @Column(nullable = false, length = 10)
    private String currency = "AZN";

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Size(max = 500, message = "Qeyd 500 simvoldan çox ola bilməz")
    private String notes;

    @Size(max = 500)
    @Column(name = "note")
    private String note;

    @PrePersist
    @PreUpdate
    private void syncLegacyFields() {
        if (title == null || title.isBlank()) {
            title = description;
        }
        if (expenseDate == null) {
            expenseDate = LocalDate.now();
        }
        if (currency == null || currency.isBlank()) {
            currency = "AZN";
        }
        if (exchangeRate == null) {
            exchangeRate = new BigDecimal("1.0000");
        }
        if ((note == null || note.isBlank()) && notes != null && !notes.isBlank()) {
            note = notes;
        }
    }
}
