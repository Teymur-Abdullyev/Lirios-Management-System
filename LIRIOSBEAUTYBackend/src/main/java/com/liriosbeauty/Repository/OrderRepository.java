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

    // Arxivlənmiş (cancelled) sifarişləri hesaba almasın
    List<Order> findByCustomerIdAndArchivedFalse(Long customerId);

    List<Order> findByEmployeeIdAndArchivedFalse(Long employeeId);

    List<Order> findByPaymentStatusAndArchivedFalse(PaymentStatus paymentStatus);

    // Ümumi borc (CANCELLED xaric)
    @Query("SELECT COALESCE(SUM(o.totalAmount - o.paidAmount), 0) FROM Order o " +
            "WHERE o.paymentStatus != 'PAID' AND o.archived = false")
    BigDecimal getTotalDebtExcludingCancelled();

        @Query("SELECT COALESCE(SUM(o.totalAmount - o.paidAmount), 0) FROM Order o " +
            "WHERE o.customer.id = :customerId AND o.paymentStatus != 'PAID' AND o.archived = false")
        BigDecimal getDebtByCustomerId(@Param("customerId") Long customerId);

    // İşçi satışları (CANCELLED xaric)
    @Query("SELECT SUM(o.totalAmount) FROM Order o " +
            "WHERE o.employee.id = :employeeId " +
            "AND o.orderedAt BETWEEN :start AND :end " +
            "AND o.archived = false")
    BigDecimal sumByEmployeeAndPeriod(
            @Param("employeeId") Long employeeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Köhnə metodlar (geriyə uyğunluq üçün)
    @Deprecated
    default List<Order> findByCustomerId(Long customerId) {
        return findByCustomerIdAndArchivedFalse(customerId);
    }

    @Deprecated
    default List<Order> findByPaymentStatus(PaymentStatus paymentStatus) {
        return findByPaymentStatusAndArchivedFalse(paymentStatus);
    }

    @Deprecated
    default BigDecimal getTotalDebt() {
        return getTotalDebtExcludingCancelled();
    }
}