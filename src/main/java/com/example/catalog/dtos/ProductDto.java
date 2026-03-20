package com.example.catalog.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductDto(
        @NotBlank String name,
        String description,
        @NotNull BigDecimal price,
        String category,
        boolean inStock) {
}
