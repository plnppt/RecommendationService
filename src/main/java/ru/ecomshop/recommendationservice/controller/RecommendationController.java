package ru.ecomshop.recommendationservice.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.ecomshop.recommendationservice.service.RecommendationService;

import java.util.Collection;

import static org.springframework.http.ResponseEntity.ok;

@Controller
@RequestMapping("/recommendations")
@AllArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/{userId}")
    public ResponseEntity<Collection<Long>> getRecommendations(@PathVariable Long userId) {
        return ok(recommendationService.getRecommendations(userId));
    }
}
