package com.liriosbeauty.Service;

import com.liriosbeauty.Entity.*;
import com.liriosbeauty.Exception.InsufficientStockException;
import com.liriosbeauty.Repository.OrderItemRepository;
import com.liriosbeauty.Repository.OrderRepository;
import com.liriosbeauty.Repository.ProductRepository;
import com.liriosbeauty.Repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Order createOrder(Order order, List<OrderItem> items, BigDecimal paidAmount) {

        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Sifariş boşdur!");
        }

        // Total hesablama
        BigDecimal total = items.stream()
                .map(i -> i.getUnitPrice()
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(total);
        order.setStatus(OrderStatus.COMPLETED);

        if (paidAmount == null) paidAmount = BigDecimal.ZERO;

        // Ödəniş validation
        if (paidAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Ödənilən məbləğ mənfi ola bilməz!");
        }

        if (paidAmount.compareTo(total) > 0) {
            throw new RuntimeException(
                String.format("Artıq ödəniş! Ümumi: %.2f AZN, Ödənilən: %.2f AZN", 
                    total, paidAmount)
            );
        }

        order.setPaidAmount(paidAmount);

        // Payment status təyin et
        if (paidAmount.compareTo(total) >= 0) {
            order.setPaymentStatus(PaymentStatus.PAID);
        } else if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            order.setPaymentStatus(PaymentStatus.DEBT);
        } else {
            order.setPaymentStatus(PaymentStatus.PARTIAL);
        }

        // Borc olduqda müştəri mütləqdir
        if (!order.getPaymentStatus().equals(PaymentStatus.PAID) && order.getCustomer() == null) {
            throw new RuntimeException("Borca satış üçün müştəri məlumatı mütləqdir!");
        }

        Order saved = orderRepository.save(order);

        // OrderItem və stok əməliyyatları
        for (OrderItem item : items) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Məhsul tapılmadı: " + item.getProduct().getId()));

            if (product.isDeleted()) {
                throw new RuntimeException("Silinmiş məhsuldan satış edilə bilməz: " + product.getName());
            }

            // Stok yoxlanışı
            if (product.getStockQty() < item.getQuantity()) {
                throw new InsufficientStockException(
                    String.format("Kifayət qədər stok yoxdur! Məhsul: %s, Tələb: %d, Mövcud: %d",
                        product.getName(), item.getQuantity(), product.getStockQty())
                );
            }

            item.setOrder(saved);
            orderItemRepository.save(item);

            // Stoku azalt
            product.setStockQty(product.getStockQty() - item.getQuantity());
            productRepository.save(product);

            // Stok hərəkətini log et
            StockMovement movement = new StockMovement();
            movement.setProduct(product);
            movement.setQuantity(-item.getQuantity());
            movement.setType(MovementType.SALE);
            stockMovementRepository.save(movement);

            log.info("Satış: {} x{} (Qalan stok: {})", product.getName(), item.getQuantity(), product.getStockQty());
        }

        log.info("Sifariş yaradıldı: ID={}, Ümumi={} AZN, Ödənildi={} AZN, Status={}", 
            saved.getId(), total, paidAmount, order.getPaymentStatus());

        return saved;
    }

    @Transactional
    public Order payDebt(Long id, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Ödəniş məbləği müsbət olmalıdır!");
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sifariş tapılmadı"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Ləğv edilmiş sifarişə ödəniş edilə bilməz!");
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Bu sifariş artıq tam ödənilib!");
        }

        // Qalan borc
        BigDecimal remaining = order.getTotalAmount().subtract(order.getPaidAmount());

        // Artıq ödəniş yoxlanışı
        if (amount.compareTo(remaining) > 0) {
            throw new RuntimeException(
                String.format("Artıq ödəniş! Qalan borc: %.2f AZN, Ödəmək istəyirsən: %.2f AZN",
                    remaining, amount)
            );
        }

        BigDecimal newPaid = order.getPaidAmount().add(amount);
        order.setPaidAmount(newPaid);

        // Status yenilə
        if (newPaid.compareTo(order.getTotalAmount()) >= 0) {
            order.setPaymentStatus(PaymentStatus.PAID);
            log.info("Sifariş #{} tam ödənildi!", order.getId());
        } else {
            order.setPaymentStatus(PaymentStatus.PARTIAL);
            log.info("Sifariş #{} qismən ödənildi: {} AZN", order.getId(), amount);
        }

        return orderRepository.save(order);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Order cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sifariş tapılmadı"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Sifariş artıq ləğv edilib");
        }

        // Statusu CANCELLED-ə keçir
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(null);

        // Stoku geri qaytar
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        for (OrderItem item : items) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElse(null);

            if (product != null && !product.isDeleted()) {
                product.setStockQty(product.getStockQty() + item.getQuantity());
                productRepository.save(product);

                // StockMovement log
                StockMovement movement = new StockMovement();
                movement.setProduct(product);
                movement.setQuantity(item.getQuantity());
                movement.setType(MovementType.ADJUSTMENT);
                stockMovementRepository.save(movement);

                log.info("Stok geri qaytarıldı: {} (+{})", product.getName(), item.getQuantity());
            } else {
                log.warn("Məhsul silinib və ya tapılmadı, stok geri qaytarıla bilmədi: {}", 
                    item.getProduct().getId());
            }
        }

        log.info("Sifariş ləğv edildi: #{}", id);

        return orderRepository.save(order);
    }

    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getByCustomer(Long customerId) {
        return orderRepository.findByCustomerIdAndStatusNot(customerId, OrderStatus.CANCELLED);
    }

    public List<Order> getDebts() {
        return orderRepository.findByPaymentStatusAndStatusNot(PaymentStatus.DEBT, OrderStatus.CANCELLED);
    }

    public List<Order> getPartials() {
        return orderRepository.findByPaymentStatusAndStatusNot(PaymentStatus.PARTIAL, OrderStatus.CANCELLED);
    }

    public BigDecimal getTotalDebt() {
        BigDecimal debt = orderRepository.getTotalDebtExcludingCancelled();
        return debt != null ? debt : BigDecimal.ZERO;
    }
}
