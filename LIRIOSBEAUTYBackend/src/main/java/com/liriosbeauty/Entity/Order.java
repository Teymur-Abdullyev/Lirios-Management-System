package com.liriosbeauty.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @NotNull(message = "Ümumi məbləğ mütləqdir")
    @DecimalMin(value = "0.01", message = "Ümumi məbləğ 0-dan böyük olmalıdır")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.COMPLETED;

    @Column(nullable = false)
    private LocalDateTime orderedAt = LocalDateTime.now();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PAID;

    @NotNull
    @DecimalMin(value = "0.00", message = "Ödənilən məbləğ mənfi ola bilməz")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    // Helper method - qalan borc
    @Transient
    public BigDecimal getRemainingDebt() {
        if (paymentStatus == PaymentStatus.PAID) {
            return BigDecimal.ZERO;
        }
        return totalAmount.subtract(paidAmount);
    }

    // Validation helper
    @PrePersist
    @PreUpdate
    private void validatePayment() {
        if (paidAmount.compareTo(totalAmount) > 0) {
            throw new RuntimeException("Ödənilən məbləğ ümumi məbləğdən çox ola bilməz!");
        }
    }
}
