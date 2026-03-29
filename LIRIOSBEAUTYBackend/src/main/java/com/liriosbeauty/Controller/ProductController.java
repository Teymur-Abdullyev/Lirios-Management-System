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
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductDTO> getAll() {
        return productService.getAll();
    }

    @GetMapping("/barcode/{code}")
    public ResponseEntity<Product> getByBarcode(@PathVariable String code) {
        return productService.findByBarcode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> create(@RequestBody Product product) {
        return ResponseEntity.ok(productService.save(product));
    }

    @PatchMapping("/barcode/{code}/stock")
    public ResponseEntity<Product> addStock(
            @PathVariable String code,
            @RequestParam int qty) {
        return ResponseEntity.ok(productService.incrementStock(code, qty));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(
            @PathVariable Long id,
            @RequestBody Product product) {
        return ResponseEntity.ok(productService.update(id, product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}