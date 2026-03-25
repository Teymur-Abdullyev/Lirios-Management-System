package com.liriosbeauty.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class EmployeeBonusDTO {
    private String employeeName;
    private BigDecimal totalSales;
    private BigDecimal bonusPercent;
    private BigDecimal bonusAmount;
    private int year;
    private int quarter;
}
