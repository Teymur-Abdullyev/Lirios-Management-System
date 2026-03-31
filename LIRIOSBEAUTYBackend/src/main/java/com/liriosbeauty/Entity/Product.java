package com.liriosbeauty.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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

    @NotBlank(message = "Məhsul adı mütləqdir")
    @Size(min = 1, max = 200, message = "Məhsul adı 1-200 simvol arası olmalıdır")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Satış qiyməti mütləqdir")
    @DecimalMin(value = "0.01", message = "Qiymət 0-dan böyük olmalıdır")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Maya dəyəri mütləqdir")
    @DecimalMin(value = "0.00", message = "Maya dəyəri mənfi ola bilməz")
    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Min(value = 0, message = "Stok mənfi ola bilməz")
    @Column(nullable = false)
    private Integer stockQty = 0;

    @Size(max = 100, message = "Kateqoriya 100 simvoldan çox ola bilməz")
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.AVAILABLE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Legacy schema stores deletion moment only.
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Transient
    public ProductStatus getStatus() {
        return deriveStatus();
    }

    @Transient
    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void setDeleted(boolean deleted) {
        this.deletedAt = deleted ? LocalDateTime.now() : null;
        this.status = deriveStatus();
    }

    // Soft delete helper
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.status = ProductStatus.DELETED;
    }

    private ProductStatus deriveStatus() {
        if (isDeleted()) return ProductStatus.DELETED;
        return (stockQty == null || stockQty == 0)
                ? ProductStatus.OUT_OF_STOCK
                : ProductStatus.AVAILABLE;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        status = deriveStatus();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        status = deriveStatus();
    }
}
