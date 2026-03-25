package com.liriosbeauty.Controller;

import com.liriosbeauty.Entity.*;
import com.liriosbeauty.Repository.CustomerRepository;
import com.liriosbeauty.Repository.EmployeeRepository;
import com.liriosbeauty.Repository.ProductRepository;
import com.liriosbeauty.Service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final ProductRepository productRepository;

    @GetMapping
    public List<Order> getAll() {
        return orderService.getAll();
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
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(orderService.payDebt(id, amount));
    }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody Map<String, Object> body) {

        Order order = new Order();

        if (body.get("employeeId") != null) {
            Long employeeId = Long.valueOf(body.get("employeeId").toString());
            employeeRepository.findById(employeeId)
                    .ifPresent(order::setEmployee);
        }

        if (body.get("customerId") != null) {
            Long customerId = Long.valueOf(body.get("customerId").toString());
            customerRepository.findById(customerId)
                    .ifPresent(order::setCustomer);
        }

        BigDecimal paidAmount = BigDecimal.ZERO;
        if (body.get("paidAmount") != null) {
            paidAmount = new BigDecimal(body.get("paidAmount").toString());
        }

        List<Map<String, Object>> itemsData =
                (List<Map<String, Object>>) body.get("items");

        List<OrderItem> items = new ArrayList<>();
        for (Map<String, Object> itemData : itemsData) {
            Long productId = Long.valueOf(itemData.get("productId").toString());
            int  quantity  = Integer.parseInt(itemData.get("quantity").toString());

            productRepository.findById(productId).ifPresent(product -> {
                OrderItem item = new OrderItem();
                item.setProduct(product);
                item.setQuantity(quantity);
                item.setUnitPrice(product.getPrice());
                items.add(item);
            });
        }

        return ResponseEntity.ok(orderService.createOrder(order, items, paidAmount));
    }
}