package com.liriosbeauty.Service;

import com.liriosbeauty.DTO.ProductDTO;
import com.liriosbeauty.Entity.MovementType;
import com.liriosbeauty.Entity.Product;
import com.liriosbeauty.Entity.StockMovement;
import com.liriosbeauty.Repository.ProductRepository;
import com.liriosbeauty.Repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    public Optional<ProductDTO> findByBarcodeDto(String barcode) {
        return findByBarcode(barcode).map(this::toDTO);
    }

    public Product save(Product product) {
        if (productRepository.existsByBarcode(product.getBarcode())) {
            throw new RuntimeException("Bu barcode artıq mövcuddur: " + product.getBarcode());
        }
        if (product.getCostPrice() == null) {
            product.setCostPrice(BigDecimal.ZERO);
        }
        return productRepository.save(product);
    }

    public ProductDTO saveDto(Product product) {
        return toDTO(save(product));
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

    public ProductDTO incrementStockDto(String barcode, int qty) {
        return toDTO(incrementStock(barcode, qty));
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
        product.setCostPrice(updated.getCostPrice() != null ? updated.getCostPrice() : BigDecimal.ZERO);
        product.setCategory(updated.getCategory());
        return productRepository.save(product);
    }

    public ProductDTO updateDto(Long id, Product updated) {
        return toDTO(update(id, updated));
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
        dto.setCostPrice(p.getCostPrice() != null ? p.getCostPrice() : BigDecimal.ZERO);
        dto.setStockQuantity(p.getStockQty());
        dto.setCategory(p.getCategory());
        return dto;
    }
    public ProductDTO adjustStockDto(Long id, int change, String reason, String note) {
        Product p = adjustStock(id, change, reason, note);
        return toDTO(p);
    }
}