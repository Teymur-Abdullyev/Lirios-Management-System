package com.liriosbeauty.Controller;

import com.liriosbeauty.Entity.*;
import com.liriosbeauty.Repository.CustomerRepository;
import com.liriosbeauty.Repository.EmployeeRepository;
import com.liriosbeauty.Repository.ProductRepository;
import com.liriosbeauty.Service.OrderService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final ProductRepository productRepository;

    @GetMapping
    public List<Order> getAll() {
        return orderService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getById(@PathVariable Long id) {
        return orderService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public List<Order> getByCustomer(@PathVariable Long customerId) {
        return orderService.getByCustomer(customerId);
    }

    @GetMapping("/debts")
    public List<Order> getDebts() {
        return orderService.getDebts();
    }

    @GetMapping("/partials")
    public List<Order> getPartials() {
        return orderService.getPartials();
    }

    @GetMapping("/total-debt")
    public ResponseEntity<BigDecimal> getTotalDebt() {
        return ResponseEntity.ok(orderService.getTotalDebt());
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<Order> payDebt(
            @PathVariable Long id,
            @RequestParam @NotNull @DecimalMin("0.01") BigDecimal amount) {
        return ResponseEntity.ok(orderService.payDebt(id, amount));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {

        // Validation
        if (!body.containsKey("items") || ((List<?>) body.get("items")).isEmpty()) {
            return ResponseEntity.badRequest().body("Sifariş boşdur!");
        }

        Order order = new Order();

        // Employee (mütləq)
        if (!body.containsKey("employeeId") || body.get("employeeId") == null) {
            return ResponseEntity.badRequest().body("İşçi seçməlisiniz!");
        }

        Long employeeId = Long.valueOf(body.get("employeeId").toString());
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("İşçi tapılmadı: " + employeeId));

        if (!employee.isActive()) {
            return ResponseEntity.badRequest().body("Seçilən işçi aktiv deyil!");
        }

        order.setEmployee(employee);

        // Customer (optional - yalnız borc olduqda lazımdır)
        if (body.containsKey("customerId") && body.get("customerId") != null) {
            Long customerId = Long.valueOf(body.get("customerId").toString());
            customerRepository.findById(customerId)
                    .ifPresent(order::setCustomer);
        }

        // Paid amount
        BigDecimal paidAmount = BigDecimal.ZERO;
        if (body.containsKey("paidAmount") && body.get("paidAmount") != null) {
            paidAmount = new BigDecimal(body.get("paidAmount").toString());
        }

        // Items
        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) body.get("items");

        List<OrderItem> items = new ArrayList<>();
        for (Map<String, Object> itemData : itemsData) {
            if (!itemData.containsKey("productId") || !itemData.containsKey("quantity")) {
                return ResponseEntity.badRequest().body("Məhsul məlumatı tam deyil!");
            }

            Long productId = Long.valueOf(itemData.get("productId").toString());
            int quantity = Integer.parseInt(itemData.get("quantity").toString());

            if (quantity <= 0) {
                return ResponseEntity.badRequest().body("Miqdar müsbət olmalıdır!");
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Məhsul tapılmadı: " + productId));

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPrice(product.getPrice());
            items.add(item);
        }

        return ResponseEntity.ok(orderService.createOrder(order, items, paidAmount));
    }
}
