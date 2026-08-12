package com.project.domain.monthly.service;

import com.project.common.response.ApiResponse;
import com.project.domain.monthly.dto.ExpenseFeedbackCreateRequestDTO;
import com.project.domain.monthly.dto.ExpenseFeedbackCreateResponseDTO;
import com.project.domain.monthly.dto.MonthlyExpenseResponseDTO;
import com.project.domain.monthly.dto.SavingMissionRecommendationRequestDTO;
import com.project.domain.monthly.dto.SavingMissionRecommendationResponseDTO;
import com.project.domain.monthly.dto.SavingMissionResponseDTO;
import org.springframework.http.ResponseEntity;

public interface MonthlyService {

    /**
     * 월별 카테고리별 지출 및 절약 추천 정보를 조회합니다.
     *
     * @param userId 사용자 ID
     * @param year 조회 연도
     * @param month 조회 월
     * @return MonthlyExpenseResponseDTO
     */
    MonthlyExpenseResponseDTO getMonthlyCategoryExpenses(Long userId, Integer year, Integer month);

    ExpenseFeedbackCreateResponseDTO createOrUpdateExpenseFeedback(
            Long userId, ExpenseFeedbackCreateRequestDTO requestDTO);

    /**
     * 월별 절약 미션 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param year 조회 연도
     * @param month 조회 월
     * @return SavingMissionResponseDTO
     */
    SavingMissionResponseDTO getMonthlySavingMissions(Long userId, Integer year, Integer month);

    /**
     * 절약 미션 상태를 '도전하기'에서 '도전중'으로 변경합니다.
     *
     * @param userId 사용자 ID
     * @param missionId 미션 ID
     */
    void startSavingMission(Long userId, Long missionId);

    /**
     * 직전 월 소비 패턴을 분석하여 절약 추천 미션을 생성합니다.
     *
     * @param requestDTO 절약 추천 미션 생성 요청 DTO
     * @return ResponseEntity<ApiResponse<SavingMissionRecommendationResponseDTO>>
     */
    ResponseEntity<ApiResponse<SavingMissionRecommendationResponseDTO>> generateSavingMissionRecommendations(SavingMissionRecommendationRequestDTO requestDTO);
}
