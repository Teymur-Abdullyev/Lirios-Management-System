package com.liriosbeauty.Controller;


import com.liriosbeauty.Entity.Expense;
import com.liriosbeauty.Service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public List<Expense> getAll() {
        return expenseService.getAll();
    }
    @GetMapping("/yearly")
    public ResponseEntity<Map<String, BigDecimal>> getYearlyExpenses(
            @RequestParam int year) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        String[] months = {"Yan", "Fev", "Mar", "Apr", "May", "İyn",
                "İyl", "Avq", "Sen", "Okt", "Noy", "Dek"};
        for (int m = 1; m <= 12; m++) {
            BigDecimal total = expenseService.getMonthlyTotal(year, m);
            result.put(months[m-1], total);
        }
        return ResponseEntity.ok(result);
    }


    @PostMapping
    public ResponseEntity<Expense> create(@RequestBody Expense expense) {
        return ResponseEntity.ok(expenseService.save(expense));
    }

    @GetMapping("/total")
    public ResponseEntity<BigDecimal> getMonthlyTotal(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(expenseService.getMonthlyTotal(year, month));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        expenseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}