package com.liriosbeauty.Controller;

import com.liriosbeauty.DTO.ProductDTO;
import com.liriosbeauty.Entity.Product;
import com.liriosbeauty.Service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
        public ResponseEntity<ProductDTO> create(@RequestBody Map<String, Object> body) {
        Product product = new Product();
        product.setBarcode(text(body.get("barcode")));
        product.setName(firstNonBlank(
            text(body.get("name")),
            text(body.get("productName"))
        ));
        product.setPrice(firstPositiveMoney(
            money(body.get("price")),
            money(body.get("sellingPrice"))
        ));
        product.setCostPrice(firstNonNegativeMoney(
            money(body.get("costPrice")),
            money(body.get("cost_price"))
        ));
        product.setCategory(text(body.get("category")));
        product.setStockQty(intValue(body.get("stockQty"), intValue(body.get("stockQuantity"), 0)));

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
            @RequestBody Map<String, Object> body) {
        Product product = new Product();
        product.setBarcode(text(body.get("barcode")));
        product.setName(firstNonBlank(
            text(body.get("name")),
            text(body.get("productName"))
        ));
        product.setPrice(firstPositiveMoney(
            money(body.get("price")),
            money(body.get("sellingPrice"))
        ));
        product.setCostPrice(firstNonNegativeMoney(
            money(body.get("costPrice")),
            money(body.get("cost_price"))
        ));
        product.setCategory(text(body.get("category")));
        product.setStockQty(intValue(body.get("stockQty"), intValue(body.get("stockQuantity"), 0)));

        return ResponseEntity.ok(productService.updateDto(id, product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private Integer intValue(Object value, Integer defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private BigDecimal money(Object value) {
        if (value == null) return null;
        try {
            String raw = String.valueOf(value).trim().replace(',', '.');
            if (raw.isBlank()) return null;
            return new BigDecimal(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private BigDecimal firstPositiveMoney(BigDecimal... values) {
        for (BigDecimal v : values) {
            if (v != null && v.compareTo(BigDecimal.ZERO) > 0) return v;
        }
        return null;
    }

    private BigDecimal firstNonNegativeMoney(BigDecimal... values) {
        for (BigDecimal v : values) {
            if (v != null && v.compareTo(BigDecimal.ZERO) >= 0) return v;
        }
        return BigDecimal.ZERO;
    }
}