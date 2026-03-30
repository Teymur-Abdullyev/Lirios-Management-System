package com.liriosbeauty.Controller;

import com.liriosbeauty.DTO.OrderDTO;
import com.liriosbeauty.Entity.*;
import com.liriosbeauty.Repository.CustomerRepository;
import com.liriosbeauty.Repository.EmployeeRepository;
import com.liriosbeauty.Repository.ProductRepository;
import com.liriosbeauty.Service.OrderService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final ProductRepository productRepository;

    @GetMapping
    public List<OrderDTO> getAll() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user
                && user.getRole() == UserRole.SELLER) {
            if (user.getEmployee() == null) {
                return List.of();
            }
            return orderService.getByEmployeeDto(user.getEmployee().getId());
        }
        return orderService.getAllDto();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getById(@PathVariable Long id) {
        return orderService.findByIdDto(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public List<OrderDTO> getByCustomer(@PathVariable Long customerId) {
        return orderService.getByCustomerDto(customerId);
    }

    @GetMapping("/debts")
    public List<OrderDTO> getDebts() {
        return orderService.getDebtsDto();
    }

    @GetMapping("/partials")
    public List<OrderDTO> getPartials() {
        return orderService.getPartialsDto();
    }

    @GetMapping("/total-debt")
    public ResponseEntity<BigDecimal> getTotalDebt() {
        return ResponseEntity.ok(orderService.getTotalDebt());
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<OrderDTO> payDebt(
            @PathVariable Long id,
            @RequestParam @NotNull @DecimalMin("0.01") BigDecimal amount) {
        return ResponseEntity.ok(orderService.payDebtDto(id, amount));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrderDto(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrderAndRestoreStock(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {

        // Validation
        if (!body.containsKey("items") || ((List<?>) body.get("items")).isEmpty()) {
            return ResponseEntity.badRequest().body("Sifariş boşdur!");
        }

        Order order = new Order();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = null;
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            currentUser = user;
        }

        if (currentUser != null && currentUser.getRole() == UserRole.SELLER) {
            if (currentUser.getEmployee() == null) {
                return ResponseEntity.badRequest().body("Seller hesabı employee ilə əlaqələndirilməlidir!");
            }
            order.setEmployee(currentUser.getEmployee());
        }

        // Employee (optional)
        if (order.getEmployee() == null && body.containsKey("employeeId") && body.get("employeeId") != null) {
            Long employeeId = Long.valueOf(body.get("employeeId").toString());
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("İşçi tapılmadı: " + employeeId));

            if (!employee.isActive()) {
                return ResponseEntity.badRequest().body("Seçilən işçi aktiv deyil!");
            }

            order.setEmployee(employee);
        }

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
        List<?> itemsData = (List<?>) body.get("items");

        List<OrderItem> items = new ArrayList<>();
        for (Object rawItem : itemsData) {
            if (!(rawItem instanceof Map<?, ?> rawMap)) {
                return ResponseEntity.badRequest().body("Məhsul məlumatı düzgün formatda deyil!");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> itemData = (Map<String, Object>) rawMap;

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

        return ResponseEntity.ok(orderService.createOrderDto(order, items, paidAmount));
    }
}
