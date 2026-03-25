package com.liriosbeauty.Repository;

import com.liriosbeauty.Entity.BonusRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface BonusRuleRepository extends JpaRepository<BonusRule, Long> {

    @Query("SELECT b FROM BonusRule b " +
            "WHERE b.minSales <= :amount " +
            "AND (b.maxSales IS NULL OR b.maxSales >= :amount)")
    Optional<BonusRule> findRuleForAmount(@Param("amount") BigDecimal amount);
}