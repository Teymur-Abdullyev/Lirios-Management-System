package com.liriosbeauty.Service;
import com.liriosbeauty.Entity.BonusRule;

import com.liriosbeauty.DTO.EmployeeBonusDTO;
import com.liriosbeauty.Repository.BonusRuleRepository;
import com.liriosbeauty.Repository.EmployeeRepository;
import com.liriosbeauty.Repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BonusService {

    private final OrderRepository orderRepository;
    private final BonusRuleRepository bonusRuleRepository;
    private final EmployeeRepository employeeRepository;

    public List<EmployeeBonusDTO> calculateQuarterlyBonus(int year, int quarter) {

        LocalDateTime start = getQuarterStart(year, quarter);
        LocalDateTime end   = getQuarterEnd(year, quarter);

        return employeeRepository.findByActiveTrue().stream()
                .map(emp -> {
                    BigDecimal totalSales = orderRepository
                            .sumByEmployeeAndPeriod(emp.getId(), start, end);

                    if (totalSales == null) totalSales = BigDecimal.ZERO;

                    BigDecimal finalTotalSales = totalSales;

                    BigDecimal bonusPercent = bonusRuleRepository
                            .findRuleForAmount(finalTotalSales)
                            .map(rule -> rule.getBonusPercent())
                            .orElse(BigDecimal.ZERO);

                    BigDecimal bonusAmount = finalTotalSales
                            .multiply(bonusPercent)
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                    return new EmployeeBonusDTO(
                            emp.getFullName(),
                            finalTotalSales,
                            bonusPercent,
                            bonusAmount,
                            year,
                            quarter
                    );
                })
                .collect(Collectors.toList());
    }

    private LocalDateTime getQuarterStart(int year, int quarter) {
        int month = (quarter - 1) * 3 + 1;
        return LocalDate.of(year, month, 1).atStartOfDay();
    }

    private LocalDateTime getQuarterEnd(int year, int quarter) {
        int month = quarter * 3;
        return LocalDate.of(year, month, 1)
                .withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth())
                .atTime(23, 59, 59);
    }
}