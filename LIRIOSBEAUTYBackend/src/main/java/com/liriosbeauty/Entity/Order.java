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
    @JoinColumn(name = "employee_id", nullable = true)
    private Employee employee;

    @NotNull(message = "Ümumi məbləğ mütləqdir")
    @DecimalMin(value = "0.01", message = "Ümumi məbləğ 0-dan böyük olmalıdır")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private boolean archived = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime orderedAt = LocalDateTime.now();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PAID;

    @NotNull
    @DecimalMin(value = "0.00", message = "Ödənilən məbləğ mənfi ola bilməz")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @NotNull
    @Column(name = "exchange_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal exchangeRate = new BigDecimal("1.0000");

    @NotNull
    @Column(nullable = false, length = 10)
    private String currency = "AZN";

    @NotNull
    @Column(name = "order_number", nullable = false, length = 40, unique = true)
    private String orderNumber;

    @Transient
    public OrderStatus getStatus() {
        return archived ? OrderStatus.CANCELLED : OrderStatus.COMPLETED;
    }

    public void setStatus(OrderStatus status) {
        this.archived = status == OrderStatus.CANCELLED;
    }

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
        if (paidAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Ödənilən məbləğ mənfi ola bilməz!");
        }
        if (orderNumber == null || orderNumber.isBlank()) {
            orderNumber = "ORD-" + System.currentTimeMillis();
        }
        if (currency == null || currency.isBlank()) {
            currency = "AZN";
        }
        if (exchangeRate == null) {
            exchangeRate = new BigDecimal("1.0000");
        }
    }
}
