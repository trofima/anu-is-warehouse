package com.isd.warehouse.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReceiveProductDto(
        @NotNull @Min(1) Integer quantity) {
}
