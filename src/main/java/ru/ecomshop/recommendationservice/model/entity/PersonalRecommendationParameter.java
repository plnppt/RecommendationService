package ru.ecomshop.recommendationservice.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "personal_recommendation_parameters")
public class PersonalRecommendationParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ParameterType parameterType;

    @Enumerated(EnumType.STRING)
    private ParameterValueType parameterValueType;

    @Column(name = "parameter_name")
    private String parameterName;

    @Column(name = "parameter_value")
    private String parameterValue;
}
