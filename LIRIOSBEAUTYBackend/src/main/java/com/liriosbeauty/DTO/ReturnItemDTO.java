package com.liriosbeauty.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReturnItemDTO {
    private Long productId;
    private Integer quantity;
    private String reason; // Qaytarma səbəbi (optional)
}