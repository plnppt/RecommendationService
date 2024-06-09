package ru.ecomshop.recommendationservice.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserResponse (
        Long id,
        Date birthdate,
        Date registrationDate) {
}
