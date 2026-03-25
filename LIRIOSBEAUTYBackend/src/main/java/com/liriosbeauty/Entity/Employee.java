package com.liriosbeauty.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
@Entity
@Getter
@Setter
public class Employee {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    private String phone;

    @Column(nullable = false)
    private BigDecimal baseSalary;

    private boolean active = true;

    private LocalDate hiredAt;
}
