package com.liriosbeauty.Controller;

import com.liriosbeauty.DTO.ProductDTO;
import com.liriosbeauty.Entity.Product;
import com.liriosbeauty.Service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductDTO> getAll() {
        return productService.getAll();
    }

    @GetMapping("/barcode/{code}")
    public ResponseEntity<ProductDTO> getByBarcode(@PathVariable String code) {
        return productService.findByBarcodeDto(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ProductDTO> create(@RequestBody Product product) {
        return ResponseEntity.ok(productService.saveDto(product));
    }

    @PatchMapping("/barcode/{code}/stock")
    public ResponseEntity<ProductDTO> addStock(
            @PathVariable String code,
            @RequestParam int qty) {
        return ResponseEntity.ok(productService.incrementStockDto(code, qty));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductDTO> updateStockById(
            @PathVariable Long id,
            @RequestParam int quantity) {
        return ResponseEntity.ok(productService.adjustStockDto(id, quantity, "MANUAL_UI", null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> update(
            @PathVariable Long id,
            @RequestBody Product product) {
        return ResponseEntity.ok(productService.updateDto(id, product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}