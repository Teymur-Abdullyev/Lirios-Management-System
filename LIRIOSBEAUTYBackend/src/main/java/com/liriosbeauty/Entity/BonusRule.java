package com.liriosbeauty.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "bonus_rules")
@Getter
@Setter
@NoArgsConstructor
public class BonusRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal minSales;

    private BigDecimal maxSales;

    @Column(nullable = false)
    private BigDecimal bonusPercent;
}