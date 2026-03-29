package com.liriosbeauty.DTO;


import com.liriosbeauty.Entity.OrderStatus;
import com.liriosbeauty.Entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private CustomerDTO customer;
    private EmployeeDTO employee;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private LocalDateTime orderedAt;
    private List<OrderItemDTO> items;
}