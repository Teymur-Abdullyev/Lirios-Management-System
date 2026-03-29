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
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal costPrice;

    @Min(value = 0, message = "Stok mənfi ola bilməz")
    @Column(nullable = false)
    private Integer stockQty = 0;

    @Size(max = 100, message = "Kateqoriya 100 simvoldan çox ola bilməz")
    private String category;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // SOFT DELETE
    @Column(nullable = false)
    private boolean deleted = false;

    private LocalDateTime deletedAt;

    @Transient
    public ProductStatus getStatus() {
        if (deleted) return ProductStatus.DELETED;
        return (stockQty == null || stockQty == 0)
                ? ProductStatus.OUT_OF_STOCK
                : ProductStatus.AVAILABLE;
    }

    // Soft delete helper
    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
