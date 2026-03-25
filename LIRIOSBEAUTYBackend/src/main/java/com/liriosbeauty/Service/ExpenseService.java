package com.liriosbeauty.Service;

import com.liriosbeauty.Entity.Expense;
import com.liriosbeauty.Repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public List<Expense> getAll() {
        return expenseRepository.findAll();
    }

    public Expense save(Expense expense) {
        return expenseRepository.save(expense);
    }

    public BigDecimal getMonthlyTotal(int year, int month) {
        BigDecimal total = expenseRepository.sumByYearAndMonth(year, month);
        return total != null ? total : BigDecimal.ZERO;
    }

    public void delete(Long id) {
        expenseRepository.deleteById(id);
    }
}