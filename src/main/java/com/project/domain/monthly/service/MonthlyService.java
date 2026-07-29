package com.project.domain.monthly.service;

import com.project.domain.monthly.dto.MonthlyExpenseResponseDTO;
import com.project.domain.monthly.dto.SavingMissionResponseDTO;

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

    /**
     * 월별 절약 미션 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param year 조회 연도
     * @param month 조회 월
     * @return SavingMissionResponseDTO
     */
    SavingMissionResponseDTO getMonthlySavingMissions(Long userId, Integer year, Integer month);
}
