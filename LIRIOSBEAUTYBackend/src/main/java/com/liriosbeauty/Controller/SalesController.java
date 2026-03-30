package com.liriosbeauty.Controller;

import com.liriosbeauty.Entity.Customer;
import com.liriosbeauty.Entity.Employee;
import com.liriosbeauty.Entity.Order;
import com.liriosbeauty.Entity.OrderItem;
import com.liriosbeauty.Entity.Product;
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
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SalesController {

    private final OrderService orderService;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final ProductRepository productRepository;

    @PostMapping("/debt")
    public ResponseEntity<?> createDebtSale(@RequestBody Map<String, Object> body) {
        if (!body.containsKey("products") || ((List<?>) body.get("products")).isEmpty()) {
            return ResponseEntity.badRequest().body("Sifariş boşdur!");
        }

        if (!body.containsKey("customerId") || body.get("customerId") == null) {
            return ResponseEntity.badRequest().body("Borclu satış üçün müştəri seçilməlidir!");
        }

        Long customerId = Long.valueOf(body.get("customerId").toString());
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Müştəri tapılmadı: " + customerId));

        Order order = new Order();
        order.setCustomer(customer);

        if (body.containsKey("employeeId") && body.get("employeeId") != null) {
            Long employeeId = Long.valueOf(body.get("employeeId").toString());
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("İşçi tapılmadı: " + employeeId));
            order.setEmployee(employee);
        }

        BigDecimal paidAmount = BigDecimal.ZERO;
        if (body.containsKey("amount") && body.get("amount") != null) {
            paidAmount = new BigDecimal(body.get("amount").toString());
        }
        if (body.containsKey("paidAmount") && body.get("paidAmount") != null) {
            paidAmount = new BigDecimal(body.get("paidAmount").toString());
        }

        List<?> itemsData = (List<?>) body.get("products");
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
