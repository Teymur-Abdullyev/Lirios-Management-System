package com.liriosbeauty.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String barcode;
    private String productName;      // ← frontend gözləyir
    private BigDecimal sellingPrice;  // ← frontend gözləyir
    private BigDecimal costPrice;
    private Integer stockQuantity;    // ← frontend gözləyir
    private String category;
}