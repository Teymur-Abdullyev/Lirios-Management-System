package com.liriosbeauty.Repository;

import com.liriosbeauty.Entity.Expense;
import com.liriosbeauty.Entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByCategory(ExpenseCategory category);
    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM expenses " +
            "WHERE EXTRACT(YEAR FROM expense_date) = :year " +
            "AND EXTRACT(MONTH FROM expense_date) = :month",
            nativeQuery = true)
    BigDecimal sumByYearAndMonth(
            @Param("year") int year,
            @Param("month") int month
    );
}