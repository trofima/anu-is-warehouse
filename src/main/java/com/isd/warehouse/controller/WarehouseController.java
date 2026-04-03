package com.isd.warehouse.controller;

import com.isd.warehouse.dtos.InventoryReceiptDto;
import com.isd.warehouse.entities.Product;
import com.isd.warehouse.dtos.ProductDto;
import com.isd.warehouse.dtos.ReceiveProductDto;
import com.isd.warehouse.entities.Inventory;
import com.isd.warehouse.repository.InventoryRepository;
import com.isd.warehouse.repository.ProductRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

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

@Tag(name = "Warehouse", description = "Product management APIs")
@RestController
@RequestMapping("/api/products")
public class WarehouseController {
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public WarehouseController(ProductRepository productRepository, InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Operation(summary = "List all products", description = "Returns a list of products with optional filtering by name, category, price range, and stock status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved product list")
    })
    @GetMapping
    public List<Product> list(
            @Parameter(description = "Filter by product name (case-insensitive partial match)")
            @RequestParam(required = false) String name,
            @Parameter(description = "Filter by category (exact match, case-insensitive)")
            @RequestParam(required = false) String category,
            @Parameter(description = "Minimum price filter")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price filter")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Filter by stock availability")
            @RequestParam(required = false) Boolean inStock) {

        return productRepository.findAll().stream()
                .filter(product -> name == null || (product.getName() != null && product.getName().toLowerCase().contains(name.toLowerCase())))
                .filter(product -> category == null || (product.getCategory() != null && product.getCategory().equalsIgnoreCase(category)))
                .filter(product -> minPrice == null || (product.getPrice() != null && product.getPrice().compareTo(minPrice) >= 0))
                .filter(product -> maxPrice == null || (product.getPrice() != null && product.getPrice().compareTo(maxPrice) <= 0))
                .filter(product -> inStock == null || product.isInStock() == inStock)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Get a product by ID", description = "Returns a single product by its unique identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<Product> get(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a new product", description = "Creates a new product in the warehouse")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Product created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content)
    })
    @PostMapping
    public ResponseEntity<Product> create(
            @Parameter(description = "Product data", required = true)
            @RequestBody @Valid ProductDto productDto) {
        Product product = new Product();
        product.setName(productDto.name());
        product.setDescription(productDto.description());
        product.setPrice(productDto.price());
        product.setCategory(productDto.category());
        product.setInStock(productDto.inStock());
        Product savedProduct = productRepository.save(product);
        return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
    }

    @Operation(summary = "Update a product", description = "Updates an existing product by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product updated successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated product data", required = true)
            @RequestBody @Valid ProductDto productDto) {
        return productRepository.findById(id)
                .map(existingProduct -> {
                    existingProduct.setName(productDto.name());
                    existingProduct.setDescription(productDto.description());
                    existingProduct.setPrice(productDto.price());
                    existingProduct.setCategory(productDto.category());
                    existingProduct.setInStock(productDto.inStock());
                    return ResponseEntity.ok(productRepository.save(existingProduct));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete a product", description = "Deletes a product by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Receive product stock", description = "Adds received quantity to the warehouse inventory for a product")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product stock received successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid receiving data", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @PostMapping("/{id}/receive")
    public ResponseEntity<InventoryReceiptDto> receive(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Receiving data", required = true)
            @RequestBody @Valid ReceiveProductDto receiveProductDto) {
        return productRepository.findById(id)
                .map(product -> {
                    Inventory inventory = inventoryRepository.findByProductId(id)
                            .orElseGet(() -> {
                                Inventory newInventory = new Inventory();
                                newInventory.setProduct(product);
                                newInventory.setReservedQuantity(0);
                                newInventory.setQuantity(0);
                                return newInventory;
                            });

                    inventory.setQuantity(inventory.getQuantity() + receiveProductDto.quantity());
                    product.setInStock(inventory.getQuantity() > inventory.getReservedQuantity());

                    Inventory savedInventory = inventoryRepository.save(inventory);
                    productRepository.save(product);

                    return ResponseEntity.ok(toInventoryReceiptDto(savedInventory));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private InventoryReceiptDto toInventoryReceiptDto(Inventory inventory) {
        return new InventoryReceiptDto(
                inventory.getProduct().getId(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getQuantity() - inventory.getReservedQuantity());
    }
}
