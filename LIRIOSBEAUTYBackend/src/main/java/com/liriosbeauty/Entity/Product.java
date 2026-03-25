package com.liriosbeauty.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
    @Table(name = "products")
    @Data
    @NoArgsConstructor
    public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String barcode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private BigDecimal costPrice;

    private Integer stockQty = 0;

    private String category;

    private LocalDateTime createdAt = LocalDateTime.now();

    @Transient
    public ProductStatus getStatus() {
        return (stockQty == null || stockQty == 0)
                ? ProductStatus.OUT_OF_STOCK
                : ProductStatus.AVAILABLE;  // ← bu sətir çatışmır
    }
}
