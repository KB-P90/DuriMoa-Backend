package com.project.domain.goal.service;

import com.project.domain.goal.dto.GoalCreateRequestDTO;
import com.project.domain.goal.dto.GoalCreateResponseDTO;
import com.project.domain.goal.dto.GoalDeleteResponseDTO;
import com.project.domain.goal.dto.GoalDecisionRequestDTO;
import com.project.domain.goal.dto.GoalResponseDTO;
import com.project.domain.goal.dto.GoalStatusUpdateResponseDTO;
import com.project.domain.goal.dto.GoalUpdateRequestDTO;
import com.project.domain.goal.dto.GoalUpdateResponseDTO;
import java.util.List;

public interface GoalService {

    GoalCreateResponseDTO createGoal(Long authenticatedUserId, GoalCreateRequestDTO request);

    List<GoalResponseDTO> getGoals(Long authenticatedUserId);

    GoalUpdateResponseDTO updateGoal(
            Long authenticatedUserId,
            Long goalId,
            GoalUpdateRequestDTO request
    );

    GoalDeleteResponseDTO deleteGoal(Long authenticatedUserId, Long goalId);

    GoalStatusUpdateResponseDTO requestMainGoal(Long authenticatedUserId, Long goalId);

    GoalStatusUpdateResponseDTO cancelMainGoalRequest(Long authenticatedUserId, Long goalId);

    GoalStatusUpdateResponseDTO decideMainGoalRequest(
            Long authenticatedUserId,
            Long goalId,
            GoalDecisionRequestDTO request
    );
}
