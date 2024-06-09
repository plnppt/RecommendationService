package ru.ecomshop.recommendationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ecomshop.recommendationservice.model.entity.NonPersonalRecommendation;

@Repository
public interface NonPersonalRecommendationRepository extends JpaRepository<NonPersonalRecommendation, Long> {
}
