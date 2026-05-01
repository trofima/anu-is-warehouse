package com.isd.warehouse.dtos;

import java.math.BigDecimal;

public record ProductResponseDto(
    Long id,
    String name,
    String description,
    BigDecimal price,
    String category,
    boolean inStock,
    int availableQuantity
) {}