package com.project.domain.goal.service;

import com.project.domain.goal.dto.GoalAllRegionsEstimateResponseDTO;
import com.project.domain.goal.dto.GoalBudgetEstimateResponseDTO;
import com.project.domain.goal.dto.GoalStatisticsResponseDTO;

public interface GoalStatisticsService {

    GoalStatisticsResponseDTO findGoalStatistics(
            String region,
            String category,
            Integer weddingMonth
    );

    GoalBudgetEstimateResponseDTO findGoalBudgetEstimate(
            String region,
            Integer weddingMonth
    );

    GoalAllRegionsEstimateResponseDTO findAllRegionsGoalBudgetEstimate();
}
