package com.liriosbeauty.Repository;

import com.liriosbeauty.Entity.Order;
import com.liriosbeauty.Entity.OrderStatus;
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

    // CANCELLED sifarişləri hesaba almasın
    List<Order> findByCustomerIdAndStatusNot(Long customerId, OrderStatus status);

    List<Order> findByEmployeeIdAndStatusNot(Long employeeId, OrderStatus status);

    List<Order> findByPaymentStatusAndStatusNot(PaymentStatus paymentStatus, OrderStatus status);

    // Ümumi borc (CANCELLED xaric)
    @Query("SELECT COALESCE(SUM(o.totalAmount - o.paidAmount), 0) FROM Order o " +
            "WHERE o.paymentStatus != 'PAID' AND o.status != 'CANCELLED'")
    BigDecimal getTotalDebtExcludingCancelled();

    // İşçi satışları (CANCELLED xaric)
    @Query("SELECT SUM(o.totalAmount) FROM Order o " +
            "WHERE o.employee.id = :employeeId " +
            "AND o.orderedAt BETWEEN :start AND :end " +
            "AND o.status = 'COMPLETED'")
    BigDecimal sumByEmployeeAndPeriod(
            @Param("employeeId") Long employeeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Köhnə metodlar (geriyə uyğunluq üçün)
    @Deprecated
    default List<Order> findByCustomerId(Long customerId) {
        return findByCustomerIdAndStatusNot(customerId, OrderStatus.CANCELLED);
    }

    @Deprecated
    default List<Order> findByPaymentStatus(PaymentStatus paymentStatus) {
        return findByPaymentStatusAndStatusNot(paymentStatus, OrderStatus.CANCELLED);
    }

    @Deprecated
    default BigDecimal getTotalDebt() {
        return getTotalDebtExcludingCancelled();
    }
}