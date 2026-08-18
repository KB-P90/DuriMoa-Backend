package com.project.domain.ai.service;

import com.project.domain.ai.dto.WeddingBudgetRecommendationRequestDTO;
import com.project.domain.ai.dto.WeddingBudgetRecommendationResponseDTO;

public interface WeddingBudgetRecommendationService {

    WeddingBudgetRecommendationResponseDTO recommend(
            Long userId,
            WeddingBudgetRecommendationRequestDTO request
    );
}
