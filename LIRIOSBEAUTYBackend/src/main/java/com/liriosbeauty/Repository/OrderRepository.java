package com.liriosbeauty.Repository;

import com.liriosbeauty.Entity.Order;
import com.liriosbeauty.Entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(Long customerId);

    List<Order> findByEmployeeId(Long employeeId);

    List<Order> findByPaymentStatus(PaymentStatus paymentStatus);

    @Query("SELECT COALESCE(SUM(o.totalAmount - o.paidAmount), 0) FROM Order o " +
            "WHERE o.paymentStatus != 'PAID'")
    BigDecimal getTotalDebt();

    @Query("SELECT SUM(o.totalAmount) FROM Order o " +
            "WHERE o.employee.id = :employeeId " +
            "AND o.orderedAt BETWEEN :start AND :end " +
            "AND o.status = 'COMPLETED'")
    BigDecimal sumByEmployeeAndPeriod(
            @Param("employeeId") Long employeeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}