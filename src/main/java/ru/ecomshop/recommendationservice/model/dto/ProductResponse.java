package ru.ecomshop.recommendationservice.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductResponse(
        Long id,
        Long categoryId,
        Long brandId
) {}
