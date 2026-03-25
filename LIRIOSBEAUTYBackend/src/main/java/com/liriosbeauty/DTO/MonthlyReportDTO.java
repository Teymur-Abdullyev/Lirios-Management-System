package com.liriosbeauty.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class MonthlyReportDTO {
    private BigDecimal revenue;
    private BigDecimal costOfGoods;
    private BigDecimal grossProfit;
    private BigDecimal totalExpenses;
    private BigDecimal netProfit;
}
