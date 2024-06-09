package ru.ecomshop.recommendationservice.model.dto;

import java.util.Date;
import java.util.List;

public record PurchaseResponse(
        Long id,
        Long userId,
        double totalAmount,
        Date purchaseDate,
        List<PurchaseItemResponse> items
) {}