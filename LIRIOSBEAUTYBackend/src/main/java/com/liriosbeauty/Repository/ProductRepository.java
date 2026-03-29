package com.liriosbeauty.Repository;

import com.liriosbeauty.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Optional<Product> findByBarcodeAndDeletedFalse(String barcode);
    
    boolean existsByBarcodeAndDeletedFalse(String barcode);
    
    List<Product> findByDeletedFalse();
    
    // Köhnə metodlar - geriyə uyğunluq üçün
    @Deprecated
    default Optional<Product> findByBarcode(String barcode) {
        return findByBarcodeAndDeletedFalse(barcode);
    }
    
    @Deprecated
    default boolean existsByBarcode(String barcode) {
        return existsByBarcodeAndDeletedFalse(barcode);
    }
    
    @Deprecated
    default List<Product> findAll() {
        return findByDeletedFalse();
    }
}
