package com.liriosbeauty.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "product_name_snapshot", nullable = false, length = 180)
    private String productNameSnapshot;

    @Column(name = "line_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice; // Satış anındakı qiymət

    @PrePersist
    @PreUpdate
    private void prepareCalculatedFields() {
        if ((productNameSnapshot == null || productNameSnapshot.isBlank()) && product != null) {
            productNameSnapshot = product.getName();
        }
        if (lineTotal == null && unitPrice != null && quantity != null) {
            lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}

