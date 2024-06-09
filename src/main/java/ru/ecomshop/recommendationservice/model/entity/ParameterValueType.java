package ru.ecomshop.recommendationservice.model.entity;

public enum ParameterValueType {
    SPECIFIC_VALUE("male"),
    RANGE_VALUES("[0,10]"),
    SPREAD_VALUES("5");

    private final String example;


    ParameterValueType(String example) {
        this.example = example;
    }
}
