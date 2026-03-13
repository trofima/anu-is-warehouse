package com.example.catalog.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spring REST controller for a Product catalog with CRUD operations
 * and a list endpoint that supports filtering.
 *
 * Note: This is an in-memory implementation for demonstration purposes.
 * In a real application, replace the in-memory store with a ProductService
 * backed by a database/repository.
 */
@RestController
@RequestMapping("/api/products")
public class WarehouseController {

    // In-memory storage for demonstration
    private final Map<Long, Product> store = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);

    // List with optional filters
    @GetMapping
    public List<Product> list(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "inStock", required = false) Boolean inStock) {

        return store.values().stream()
                .filter(product -> name == null || (product.getName() != null && product.getName().toLowerCase().contains(name.toLowerCase())))
                .filter(product -> category == null || (product.getCategory() != null && product.getCategory().equalsIgnoreCase(category)))
                .filter(product -> minPrice == null || (product.getPrice() != null && product.getPrice().compareTo(minPrice) >= 0))
                .filter(product -> maxPrice == null || (product.getPrice() != null && product.getPrice().compareTo(maxPrice) <= 0))
                .filter(product -> inStock == null || product.isInStock() == inStock)
                .collect(Collectors.toList());
    }

    // Get by id
    @GetMapping("/{id}")
    public ResponseEntity<Product> get(@PathVariable Long id) {
        Product product = store.get(id);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    // Create
    @PostMapping
    public ResponseEntity<Product> create(@RequestBody @Valid ProductDTO dto) {
        Product product = new Product();
        product.setId(idSeq.getAndIncrement());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setCategory(dto.getCategory());
        product.setInStock(dto.isInStock());
        store.put(product.getId(), product);
        return new ResponseEntity<>(product, HttpStatus.CREATED);
    }

    // Update
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody @Valid ProductDTO dto) {
        Product existing = store.get(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setPrice(dto.getPrice());
        existing.setCategory(dto.getCategory());
        existing.setInStock(dto.isInStock());
        return ResponseEntity.ok(existing);
    }

    // Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Product removed = store.remove(id);
        if (removed == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    // In-memory product entity
    public static class Product {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private String category;
        private boolean inStock;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public boolean isInStock() { return inStock; }
        public void setInStock(boolean inStock) { this.inStock = inStock; }
    }

    // DTO for create/update requests
    public static class ProductDTO {
        @NotBlank
        private String name;
        private String description;
        @NotNull
        private BigDecimal price;
        private String category;
        private boolean inStock;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public boolean isInStock() { return inStock; }
        public void setInStock(boolean inStock) { this.inStock = inStock; }
    }
}
