package com.project.domain.monthly.service;

import com.project.common.enums.ExpenseCategory;
import com.project.common.enums.MissionStatus;
import com.project.common.response.ApiResponse;
import com.project.domain.monthly.dto.CategoryExpenseAnalysisDTO;
import com.project.domain.monthly.dto.ExpenseCategoryDTO;
import com.project.domain.monthly.dto.MonthlyExpenseResponseDTO;
import com.project.domain.monthly.dto.RecommendedMissionDTO;
import com.project.domain.monthly.dto.SavingMissionChallengeRequestDTO;
import com.project.domain.monthly.dto.SavingMissionDTO;
import com.project.domain.monthly.dto.SavingMissionEntityDTO;
import com.project.domain.monthly.dto.SavingMissionRecommendationRequestDTO;
import com.project.domain.monthly.dto.SavingMissionRecommendationResponseDTO;
import com.project.domain.monthly.dto.SavingMissionResponseDTO;
import com.project.domain.monthly.mapper.MonthlyMapper;
import com.project.domain.monthly.domain.GeneratedMissionInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonthlyServiceImpl implements MonthlyService {

    private final MonthlyMapper monthlyMapper;

    @Override
    @Transactional(readOnly = true)
    public MonthlyExpenseResponseDTO getMonthlyCategoryExpenses(Long userId, Integer year, Integer month) {

        List<ExpenseCategoryDTO> categories = monthlyMapper.selectMonthlyExpenseCategories(userId, year, month);
        
        if (categories == null) {
            categories = Collections.emptyList();
        }

        return MonthlyExpenseResponseDTO.builder()
                .year(year)
                .expenseCategories(categories)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SavingMissionResponseDTO getMonthlySavingMissions(Long userId, Integer year, Integer month) {
        List<SavingMissionDTO> missions = monthlyMapper.selectMonthlySavingMissions(userId, year, month);

        if (missions == null) {
            missions = Collections.emptyList();
        }

        long totalExpectedSavingAmount = missions.stream()
                .mapToLong(m -> m.getExpectedSavingAmount() != null ? m.getExpectedSavingAmount() : 0L)
                .sum();

        String yearMonth = String.format("%04d-%02d", year, month);

        return SavingMissionResponseDTO.builder()
                .yearMonth(yearMonth)
                .totalExpectedSavingAmount(totalExpectedSavingAmount)
                .missions(missions)
                .build();
    }

    @Override
    @Transactional
    public void startSavingMission(Long userId, Long missionId, SavingMissionChallengeRequestDTO requestDTO) {
        monthlyMapper.updateSavingMissionStatusToInProgress(userId, missionId);
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<SavingMissionRecommendationResponseDTO>> generateSavingMissionRecommendations(SavingMissionRecommendationRequestDTO requestDTO) {
        Long userId = requestDTO.getUserId();
        Integer targetYear = requestDTO.getTargetYear();
        Integer targetMonth = requestDTO.getTargetMonth();

        YearMonth targetYM = YearMonth.of(targetYear, targetMonth);
        YearMonth analysisYM = targetYM.minusMonths(1);

        int analysisYear = analysisYM.getYear();
        int analysisMonth = analysisYM.getMonthValue();

        LocalDate startDate = analysisYM.atDay(1);
        LocalDate endDate = targetYM.atDay(1);

        List<CategoryExpenseAnalysisDTO> analysisList = monthlyMapper.selectCategoryExpenseAnalysis(userId, startDate, endDate);

        if (analysisList == null || analysisList.isEmpty()) {
            SavingMissionRecommendationResponseDTO responseDTO = SavingMissionRecommendationResponseDTO.builder()
                    .userId(userId)
                    .analysisYear(analysisYear)
                    .analysisMonth(analysisMonth)
                    .targetYear(targetYear)
                    .targetMonth(targetMonth)
                    .missionCount(0)
                    .missions(Collections.emptyList())
                    .build();

            return ResponseEntity.ok(ApiResponse.success("분석할 소비 내역이 없어 추천 미션이 생성되지 않았습니다.", responseDTO));
        }

        List<CategoryExpenseAnalysisDTO> topCategories = analysisList.stream()
                .limit(4)
                .toList();

        LocalDateTime targetCreatedAt = targetYM.atDay(1).atTime(10, 0, 0);

        List<RecommendedMissionDTO> recommendedMissions = new ArrayList<>();

        for (CategoryExpenseAnalysisDTO analysis : topCategories) {
            String categoryCode = analysis.getCategoryCode();
            Long totalAmount = analysis.getTotalAmount() != null ? analysis.getTotalAmount() : 0L;
            Integer txCount = analysis.getTxCount() != null ? analysis.getTxCount() : 1;

            GeneratedMissionInfoVO missionInfo = createMission(categoryCode, totalAmount, txCount);

            SavingMissionEntityDTO entity = SavingMissionEntityDTO.builder()
                    .userId(userId)
                    .categoryCode(categoryCode)
                    .title(missionInfo.getTitle())
                    .expectedSavingAmount(missionInfo.getExpectedSavingAmount())
                    .status(MissionStatus.CHALLENGE.getStatus())
                    .createdAt(targetCreatedAt)
                    .build();

            monthlyMapper.insertSavingMission(entity);

            recommendedMissions.add(RecommendedMissionDTO.builder()
                    .missionId(entity.getId())
                    .categoryCode(categoryCode)
                    .title(missionInfo.getTitle())
                    .expectedSavingAmount(missionInfo.getExpectedSavingAmount())
                    .build());
        }

        SavingMissionRecommendationResponseDTO responseDTO = SavingMissionRecommendationResponseDTO.builder()
                .userId(userId)
                .analysisYear(analysisYear)
                .analysisMonth(analysisMonth)
                .targetYear(targetYear)
                .targetMonth(targetMonth)
                .missionCount(recommendedMissions.size())
                .missions(recommendedMissions)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("절약 추천 미션이 생성되었습니다.", responseDTO));
    }

    private GeneratedMissionInfoVO createMission(String categoryCode, Long totalAmount, Integer txCount) {
        if (categoryCode == null) {
            categoryCode = ExpenseCategory.ETC.name();
        }
        if (totalAmount == null) {
            totalAmount = 0L;
        }
        if (txCount == null || txCount < 1) {
            txCount = 1;
        }

        String code = categoryCode.toUpperCase();
        String title;
        long expectedSavingAmount;

        // 1. 구독/정기 지출 카테고리 (SUBSCRIPTION)
        if (ExpenseCategory.SUBSCRIPTION.name().equals(code)) {
            title = "사용하지 않는 구독 정리하기";
            expectedSavingAmount = totalAmount;
        }
        // 2. 지출 횟수가 2회 이상인 경우 (빈도 절감 미션: N회 -> N/2회)
        else if (txCount >= 2) {
            int targetCount = Math.max(1, txCount / 2);
            int reducedCount = txCount - targetCount;
            long avgPerTx = totalAmount / txCount;
            long rawSaving = reducedCount * avgPerTx;

            expectedSavingAmount = Math.max(10000L, Math.round(rawSaving / 10000.0) * 10000L);

            if (ExpenseCategory.CAFE.name().equals(code) && txCount <= 5) {
                int weeklyCount = Math.max(1, txCount);
                int targetWeekly = Math.max(1, weeklyCount / 2);
                title = String.format("주 %d회에서 주 %d회로 줄이기", weeklyCount, targetWeekly);
            } else if (ExpenseCategory.DELIVERY.name().equals(code)) {
                title = String.format("월 %d회에서 월 %d회로 줄이기", txCount, targetCount);
            } else {
                title = String.format("월 %d회에서 월 %d회로 줄이기", txCount, targetCount);
            }
        }
        // 3. 단건 지출이거나 횟수가 1회인 경우
        else {
            if (ExpenseCategory.SHOPPING.name().equals(code)) {
                title = "계획 없는 쇼핑 줄이기";
            } else {
                title = "불필요한 지출 줄이기";
            }
            expectedSavingAmount = totalAmount;
        }

        return GeneratedMissionInfoVO.builder()
                .title(title)
                .expectedSavingAmount(expectedSavingAmount)
                .build();
    }
}
