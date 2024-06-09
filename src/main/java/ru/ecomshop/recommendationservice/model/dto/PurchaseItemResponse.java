package ru.ecomshop.recommendationservice.model.dto;

public record PurchaseItemResponse(
        Long id,
        Long productId,
        int quantity
) {}
