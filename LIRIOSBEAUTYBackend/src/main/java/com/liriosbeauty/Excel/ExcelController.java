package com.liriosbeauty.Excel;

import com.liriosbeauty.DTO.MonthlyReportDTO;
import com.liriosbeauty.Entity.Expense;
import com.liriosbeauty.Entity.OrderItem;
import com.liriosbeauty.Repository.ExpenseRepository;
import com.liriosbeauty.Repository.OrderItemRepository;
import com.liriosbeauty.Repository.OrderRepository;
import com.liriosbeauty.Service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExcelController {

    private final ExcelExportService excelExportService;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseService expenseService;

    @GetMapping("/products")
    public ResponseEntity<byte[]> exportProducts() throws Exception {
        byte[] data = excelExportService.exportProducts();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=products.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/monthly")
    public ResponseEntity<byte[]> exportMonthly(
            @RequestParam int year,
            @RequestParam int month) throws Exception {
        byte[] data = excelExportService.exportMonthlyReport(year, month);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=monthly-report.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/bonus")
    public ResponseEntity<byte[]> exportBonus(
            @RequestParam int year,
            @RequestParam int quarter) throws Exception {
        byte[] data = excelExportService.exportBonusReport(year, quarter);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=bonus-report.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/monthly-data")
    public ResponseEntity<MonthlyReportDTO> getMonthlyData(
            @RequestParam int year,
            @RequestParam int month) {

        List<OrderItem> items = orderItemRepository.findByYearAndMonth(year, month);

        BigDecimal revenue = items.stream()
                .map(this::lineRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal costOfGoods = items.stream()
                .map(this::lineCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grossProfit  = revenue.subtract(costOfGoods);
        BigDecimal totalExpense = expenseService.getMonthlyTotal(year, month);
        BigDecimal netProfit    = grossProfit.subtract(totalExpense);

        return ResponseEntity.ok(new MonthlyReportDTO(
                revenue,
                costOfGoods,
                grossProfit,
                totalExpense,
                netProfit
        ));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        // CANCELLED sifarişlər çıxılaraq hesabla
        List<OrderItem> activeItems = orderItemRepository.findByOrderArchivedFalse();

        // Ümumi satış gəliri
        BigDecimal totalRevenue = activeItems.stream()
                .map(this::lineRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Ümumi mal xərci (alış qiyməti * satılan miqdar)
        BigDecimal totalCost = activeItems.stream()
                .map(this::lineCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Ümumi əməliyyat xərcləri
        BigDecimal totalExpenses = expenseRepository.findAll().stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Ümumi net qazanc
        BigDecimal netProfit = totalRevenue.subtract(totalCost).subtract(totalExpenses);

        // Ümumi borc
        BigDecimal totalDebt = orderRepository.getTotalDebtExcludingCancelled();

        return ResponseEntity.ok(Map.of(
                "totalRevenue",  totalRevenue,
                "totalCost",     totalCost,
                "totalExpenses", totalExpenses,
                "netProfit",     netProfit,
                "totalDebt",     totalDebt != null ? totalDebt : BigDecimal.ZERO
        ));
    }

        private BigDecimal lineRevenue(OrderItem item) {
                BigDecimal unitPrice = safeMoney(item.getUnitPrice());
                BigDecimal qty = BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity());
                return unitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);
        }

        private BigDecimal lineCost(OrderItem item) {
                BigDecimal costPrice = BigDecimal.ZERO;
                if (item.getProduct() != null) {
                        costPrice = safeMoney(item.getProduct().getCostPrice());
                }
                BigDecimal qty = BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity());
                return costPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);
        }

        private BigDecimal safeMoney(BigDecimal value) {
                return value == null ? BigDecimal.ZERO : value;
        }
}