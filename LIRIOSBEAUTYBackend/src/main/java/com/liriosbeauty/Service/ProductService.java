package com.liriosbeauty.Service;
import com.liriosbeauty.Entity.MovementType;
import com.liriosbeauty.Entity.Product;
import com.liriosbeauty.Entity.StockMovement;
import com.liriosbeauty.Repository.ProductRepository;
import com.liriosbeauty.Repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    // Stok manual düzəliş
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    public Optional<Product> findByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode);
    }

    public Product save(Product product) {
        if (productRepository.existsByBarcode(product.getBarcode())) {
            throw new RuntimeException("Bu barcode artıq mövcuddur: " + product.getBarcode());
        }
        return productRepository.save(product);
    }


    public Product incrementStock(String barcode, int qty) {
        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new RuntimeException("Məhsul tapılmadı: " + barcode));

        product.setStockQty(product.getStockQty() + qty);
        productRepository.save(product);

        // Stok hərəkətini log et
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setQuantity(qty);
        movement.setType(MovementType.SCAN_IN);
        stockMovementRepository.save(movement);

        return product;
    }

    public Product update(Long id, Product updated) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Məhsul tapılmadı"));
        product.setName(updated.getName());
        product.setPrice(updated.getPrice());
        product.setCostPrice(updated.getCostPrice());
        product.setCategory(updated.getCategory());
        return productRepository.save(product);
    }
    public Product adjustStock(Long id, int qty) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Məhsul tapılmadı"));
        product.setStockQty(product.getStockQty() + qty);
        if (product.getStockQty() < 0) product.setStockQty(0);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setQuantity(qty);
        movement.setType(qty > 0 ? MovementType.SCAN_IN : MovementType.ADJUSTMENT);
        stockMovementRepository.save(movement);

        return productRepository.save(product);
    }


    public void delete(Long id) {
        productRepository.deleteById(id);
    }
}