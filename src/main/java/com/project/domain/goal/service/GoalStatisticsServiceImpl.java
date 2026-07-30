package com.project.domain.goal.service;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.goal.domain.GoalStatisticsVO;
import com.project.domain.goal.dto.GoalStatisticsResponseDTO;
import com.project.domain.goal.mapper.GoalStatisticsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoalStatisticsServiceImpl implements GoalStatisticsService {

    private final GoalStatisticsMapper goalStatisticsMapper;

    @Override
    @Transactional(readOnly = true)
    public GoalStatisticsResponseDTO findGoalStatistics(String region, String category) {
        String normalizedRegion = normalizeRequired(region, "지역을 입력해 주세요.");
        String normalizedCategory = normalizeRequired(category, "카테고리를 입력해 주세요.");

        GoalStatisticsVO statistics =
                goalStatisticsMapper.selectGoalStatistics(normalizedRegion, normalizedCategory);
        if (statistics == null) {
            throw new CustomException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "조건에 해당하는 결혼 통계 정보를 찾을 수 없습니다."
            );
        }
        return GoalStatisticsResponseDTO.from(statistics);
    }

    private String normalizeRequired(String value, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, errorMessage);
        }
        return value.trim();
    }
}
