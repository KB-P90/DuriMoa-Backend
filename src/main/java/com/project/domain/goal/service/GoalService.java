package com.project.domain.goal.service;

import com.project.domain.goal.dto.GoalCreateRequestDTO;
import com.project.domain.goal.dto.GoalCreateResponseDTO;

public interface GoalService {

    GoalCreateResponseDTO createGoal(Long authenticatedUserId, GoalCreateRequestDTO request);
}
