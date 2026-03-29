package com.liriosbeauty.Service;

import com.liriosbeauty.DTO.ProductDTO;
import com.liriosbeauty.Entity.MovementType;
import com.liriosbeauty.Entity.Product;
import com.liriosbeauty.Entity.StockMovement;
import com.liriosbeauty.Repository.ProductRepository;
import com.liriosbeauty.Repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    public List<ProductDTO> getAll() {
        return productRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
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

    public Product adjustStock(Long id, int change, String reason, String note) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Məhsul tapılmadı"));

        int newStock = product.getStockQty() + change;

        if (newStock < 0) {
            throw new RuntimeException("Stok mənfi ola bilməz!");
        }

        product.setStockQty(newStock);

        // StockMovement log
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setQuantity(change);
        movement.setType(MovementType.ADJUSTMENT);
        stockMovementRepository.save(movement);

        return productRepository.save(product);
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

    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    // DTO mapping helper
    private ProductDTO toDTO(Product p) {
        ProductDTO dto = new ProductDTO();
        dto.setId(p.getId());
        dto.setBarcode(p.getBarcode());
        dto.setProductName(p.getName());
        dto.setSellingPrice(p.getPrice());
        dto.setCostPrice(p.getCostPrice());
        dto.setStockQuantity(p.getStockQty());
        dto.setCategory(p.getCategory());
        return dto;
    }
}