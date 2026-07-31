package com.project.domain.goal.service;

import com.project.domain.goal.dto.GoalCreateRequestDTO;
import com.project.domain.goal.dto.GoalCreateResponseDTO;
import com.project.domain.goal.dto.GoalResponseDTO;
import java.util.List;

public interface GoalService {

    GoalCreateResponseDTO createGoal(Long authenticatedUserId, GoalCreateRequestDTO request);

    List<GoalResponseDTO> getGoals(Long authenticatedUserId);
}
