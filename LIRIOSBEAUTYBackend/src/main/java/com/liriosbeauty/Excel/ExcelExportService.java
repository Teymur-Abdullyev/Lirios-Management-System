package com.liriosbeauty.Excel;

import com.liriosbeauty.DTO.EmployeeBonusDTO;
import com.liriosbeauty.Entity.*;
import com.liriosbeauty.Repository.*;
import com.liriosbeauty.Service.BonusService;
import com.liriosbeauty.Service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final ExpenseService expenseService;
    private final BonusService bonusService;

    // ─── 1. Məhsul siyahısı ───────────────────────────────
    @Transactional(readOnly = true)
    public byte[] exportProducts() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Məhsullar");

        // Header
        Row header = sheet.createRow(0);
        String[] cols = {"ID", "Barcode", "Ad", "Satış qiyməti", "Alış qiyməti", "Stok", "Kateqoriya", "Status"};
        for (int i = 0; i < cols.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle(workbook));
        }

        // Data
        List<Product> products = productRepository.findByDeletedAtIsNull();
        int rowNum = 1;
        for (Product p : products) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(p.getId());
            row.createCell(1).setCellValue(p.getBarcode() != null ? p.getBarcode() : "—");
            row.createCell(2).setCellValue(p.getName());
            row.createCell(3).setCellValue(safeMoney(p.getPrice()).doubleValue());
            row.createCell(4).setCellValue(safeMoney(p.getCostPrice()).doubleValue());
            row.createCell(5).setCellValue(p.getStockQty());
            row.createCell(6).setCellValue(p.getCategory() != null ? p.getCategory() : "—");
            row.createCell(7).setCellValue(p.getStatus().toString());
        }

        autoSize(sheet, cols.length);
        return toBytes(workbook);
    }

    // ─── 2. Aylıq hesabat ─────────────────────────────────
    @Transactional(readOnly = true)
    public byte[] exportMonthlyReport(int year, int month) throws IOException {
        validateMonth(month);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Aylıq hesabat");

        // Gəlir hesabla
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

        // Cədvəl
        String[][] data = {
                {"Göstərici", "Məbləğ (AZN)"},
                {"Satış gəliri",    revenue.toString()},
                {"Mal xərci",       costOfGoods.toString()},
                {"Brut mənfəət",    grossProfit.toString()},
                {"Əməliyyat xərcləri", totalExpense.toString()},
                {"Xalis mənfəət",   netProfit.toString()}
        };

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i);
            for (int j = 0; j < data[i].length; j++) {
                Cell cell = row.createCell(j);
                cell.setCellValue(data[i][j]);
                if (i == 0) cell.setCellStyle(headerStyle(workbook));
                if (i == data.length - 1) cell.setCellStyle(boldStyle(workbook));
            }
        }

        autoSize(sheet, 2);
        return toBytes(workbook);
    }

    // ─── 3. Rüblük bonus hesabatı ─────────────────────────
    @Transactional(readOnly = true)
    public byte[] exportBonusReport(int year, int quarter) throws IOException {
        validateQuarter(quarter);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Bonus hesabatı");

        Row header = sheet.createRow(0);
        String[] cols = {"İşçi", "Satış (AZN)", "Bonus %", "Bonus (AZN)"};
        for (int i = 0; i < cols.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle(workbook));
        }

        List<EmployeeBonusDTO> bonuses = bonusService.calculateQuarterlyBonus(year, quarter);
        int rowNum = 1;
        for (EmployeeBonusDTO b : bonuses) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(b.getEmployeeName());
            row.createCell(1).setCellValue(safeMoney(b.getTotalSales()).doubleValue());
            row.createCell(2).setCellValue(safeMoney(b.getBonusPercent()).doubleValue());
            row.createCell(3).setCellValue(safeMoney(b.getBonusAmount()).doubleValue());
        }

        autoSize(sheet, cols.length);
        return toBytes(workbook);
    }

    // ─── Köməkçi metodlar ─────────────────────────────────
    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle boldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void autoSize(Sheet sheet, int colCount) {
        for (int i = 0; i < colCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private byte[] toBytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    private BigDecimal lineRevenue(OrderItem item) {
        BigDecimal unitPrice = safeMoney(item.getUnitPrice());
        BigDecimal quantity = BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity());
        return unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal lineCost(OrderItem item) {
        BigDecimal costPrice = BigDecimal.ZERO;
        if (item.getProduct() != null) {
            costPrice = safeMoney(item.getProduct().getCostPrice());
        }
        BigDecimal quantity = BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity());
        return costPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void validateQuarter(int quarter) {
        if (quarter < 1 || quarter > 4) {
            throw new RuntimeException("Rüb 1-4 aralığında olmalıdır");
        }
    }

    private void validateMonth(int month) {
        if (month < 1 || month > 12) {
            throw new RuntimeException("Ay 1-12 aralığında olmalıdır");
        }
    }
}