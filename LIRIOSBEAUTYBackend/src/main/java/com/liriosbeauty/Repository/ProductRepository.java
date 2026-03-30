package com.liriosbeauty.Repository;

import com.liriosbeauty.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Optional<Product> findByBarcodeAndDeletedAtIsNull(String barcode);
    
    boolean existsByBarcodeAndDeletedAtIsNull(String barcode);
    
    List<Product> findByDeletedAtIsNull();
    
    // Köhnə metodlar - geriyə uyğunluq üçün
    @Deprecated
    default Optional<Product> findByBarcode(String barcode) {
        return findByBarcodeAndDeletedAtIsNull(barcode);
    }
    
    @Deprecated
    default boolean existsByBarcode(String barcode) {
        return existsByBarcodeAndDeletedAtIsNull(barcode);
    }
    
    @Deprecated
    default List<Product> findAll() {
        return findByDeletedAtIsNull();
    }
}
