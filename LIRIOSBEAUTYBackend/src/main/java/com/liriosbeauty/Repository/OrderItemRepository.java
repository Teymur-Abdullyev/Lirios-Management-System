package com.liriosbeauty.Repository;

import com.liriosbeauty.Entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    // CANCELLED sifarişləri hesabata daxil etmə
    @Query(value = "SELECT oi.* FROM order_items oi " +
            "JOIN orders o ON o.id = oi.order_id " +
            "WHERE EXTRACT(YEAR FROM o.ordered_at) = :year " +
            "AND EXTRACT(MONTH FROM o.ordered_at) = :month " +
            "AND o.status = 'COMPLETED'",  // ← Sadəcə COMPLETED
            nativeQuery = true)
    List<OrderItem> findByYearAndMonth(
            @Param("year") int year,
            @Param("month") int month
    );
}