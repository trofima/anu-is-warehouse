package com.isd.warehouse.dtos;

public record InventoryReceiptDto(
        Long productId,
        int quantity,
        int reservedQuantity,
        int availableQuantity) {
}
