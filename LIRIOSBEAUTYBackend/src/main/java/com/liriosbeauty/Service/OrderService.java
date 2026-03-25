package com.liriosbeauty.Service;

import com.liriosbeauty.Entity.*;
import com.liriosbeauty.Repository.OrderItemRepository;
import com.liriosbeauty.Repository.OrderRepository;
import com.liriosbeauty.Repository.ProductRepository;
import com.liriosbeauty.Repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    public Order createOrder(Order order, List<OrderItem> items, BigDecimal paidAmount) {

        BigDecimal total = items.stream()
                .map(i -> i.getUnitPrice()
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(total);
        order.setStatus(OrderStatus.COMPLETED);

        if (paidAmount == null) paidAmount = BigDecimal.ZERO;
        order.setPaidAmount(paidAmount);

        if (paidAmount.compareTo(total) >= 0) {
            order.setPaymentStatus(PaymentStatus.PAID);
        } else if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            order.setPaymentStatus(PaymentStatus.DEBT);
        } else {
            order.setPaymentStatus(PaymentStatus.PARTIAL);
        }

        Order saved = orderRepository.save(order);

        for (OrderItem item : items) {
            item.setOrder(saved);
            orderItemRepository.save(item);

            Product product = item.getProduct();
            product.setStockQty(product.getStockQty() - item.getQuantity());
            productRepository.save(product);

            StockMovement movement = new StockMovement();
            movement.setProduct(product);
            movement.setQuantity(-item.getQuantity());
            movement.setType(MovementType.SALE);
            stockMovementRepository.save(movement);
        }

        return saved;
    }

    public Order payDebt(Long id, BigDecimal amount) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sifariş tapılmadı"));

        BigDecimal newPaid = order.getPaidAmount().add(amount);
        order.setPaidAmount(newPaid);

        if (newPaid.compareTo(order.getTotalAmount()) >= 0) {
            order.setPaymentStatus(PaymentStatus.PAID);
        } else {
            order.setPaymentStatus(PaymentStatus.PARTIAL);
        }

        return orderRepository.save(order);
    }

    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    public List<Order> getByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    public List<Order> getDebts() {
        return orderRepository.findByPaymentStatus(PaymentStatus.DEBT);
    }

    public List<Order> getPartials() {
        return orderRepository.findByPaymentStatus(PaymentStatus.PARTIAL);
    }

    public BigDecimal getTotalDebt() {
        BigDecimal debt = orderRepository.getTotalDebt();
        return debt != null ? debt : BigDecimal.ZERO;
    }
}