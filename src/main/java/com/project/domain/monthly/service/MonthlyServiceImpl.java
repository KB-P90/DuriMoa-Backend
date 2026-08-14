package com.project.domain.monthly.service;

import com.project.common.enums.ExpenseCategory;
import com.project.common.enums.MissionStatus;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.common.response.ApiResponse;
import com.project.domain.monthly.dto.CategoryExpenseAnalysisDTO;
import com.project.domain.monthly.dto.CategoryExpenseStatisticsDTO;
import com.project.domain.monthly.dto.ExpenseCategoryDTO;
import com.project.domain.monthly.dto.ExpenseFeedbackDTO;
import com.project.domain.monthly.dto.ExpenseFeedbackCreateRequestDTO;
import com.project.domain.monthly.dto.ExpenseFeedbackCreateResponseDTO;
import com.project.domain.monthly.dto.ExpenseFeedbackEntityDTO;
import com.project.domain.monthly.dto.MonthlyExpenseResponseDTO;
import com.project.domain.monthly.dto.RawCategoryExpenseDTO;
import com.project.domain.monthly.dto.RecommendedMissionDTO;
import com.project.domain.monthly.dto.SavingMissionDTO;
import com.project.domain.monthly.dto.SavingMissionEntityDTO;
import com.project.domain.monthly.dto.SavingMissionRecommendationRequestDTO;
import com.project.domain.monthly.dto.SavingMissionRecommendationResponseDTO;
import com.project.domain.monthly.dto.SavingMissionResponseDTO;
import com.project.domain.monthly.dto.UserExpenseDetailDTO;
import com.project.domain.monthly.dto.UserSummaryDTO;
import com.project.domain.monthly.mapper.MonthlyMapper;
import com.project.domain.monthly.domain.GeneratedMissionInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MonthlyServiceImpl implements MonthlyService {

    private final MonthlyMapper monthlyMapper;

    @Override
    @Transactional(readOnly = true)
    public MonthlyExpenseResponseDTO getMonthlyCategoryExpenses(Long userId, Integer year, Integer month) {
        UserExpenseDetailDTO me = buildUserExpenseDetail(userId, year, month);

        Long partnerId = monthlyMapper.selectConnectedPartnerId(userId);
        UserExpenseDetailDTO partner = null;
        if (partnerId != null) {
            partner = buildUserExpenseDetail(partnerId, year, month);
        }

        List<ExpenseFeedbackDTO> feedbacks = buildMonthlyFeedbacks(
                userId, partnerId, me, partner, year, month);

        return MonthlyExpenseResponseDTO.builder()
                .year(year)
                .month(month)
                .me(me)
                .partner(partner)
                .feedbacks(feedbacks)
                .build();
    }

    private List<ExpenseFeedbackDTO> buildMonthlyFeedbacks(
            Long userId,
            Long partnerId,
            UserExpenseDetailDTO me,
            UserExpenseDetailDTO partner,
            Integer year,
            Integer month) {
        List<ExpenseFeedbackDTO> feedbacks = monthlyMapper.selectMonthlyFeedbacks(
                userId, partnerId, year, month);
        if (feedbacks != null && !feedbacks.isEmpty()) {
            return feedbacks;
        }

        List<ExpenseFeedbackDTO> emptyFeedbacks = new ArrayList<>();
        addEmptyFeedback(emptyFeedbacks, partner);
        addEmptyFeedback(emptyFeedbacks, me);
        return emptyFeedbacks;
    }

    private void addEmptyFeedback(
            List<ExpenseFeedbackDTO> feedbacks, UserExpenseDetailDTO writer) {
        if (writer == null) {
            return;
        }

        feedbacks.add(ExpenseFeedbackDTO.builder()
                .writerUserId(writer.getUserId())
                .writerName(writer.getName())
                .receiverId(writer.getUserId())
                .content("")
                .build());
    }

    private UserExpenseDetailDTO buildUserExpenseDetail(Long targetUserId, Integer year, Integer month) {
        if (targetUserId == null) {
            return null;
        }

        UserSummaryDTO userSummary = monthlyMapper.selectUserSummary(targetUserId);
        if (userSummary == null) {
            return null;
        }

        List<RawCategoryExpenseDTO> rawExpenses = monthlyMapper.selectCategoryExpensesByUserAndMonth(targetUserId, year, month);
        if (rawExpenses == null) {
            rawExpenses = Collections.emptyList();
        }

        Long monthlyIncomeBase = monthlyMapper.selectMonthlyIncomeBase(
                targetUserId, year, month);

        List<ExpenseCategoryDTO> expenseCategories = new ArrayList<>();
        long categoryId = 1L;

        for (RawCategoryExpenseDTO raw : rawExpenses) {
            String code = raw.getCategoryCode();
            if (isWeddingCategory(code)) {
                continue;
            }

            Long amount = raw.getAmount() != null ? raw.getAmount() : 0L;
            String categoryName = resolveCategoryName(code);

            int rate = 0;
            if (monthlyIncomeBase != null && monthlyIncomeBase > 0) {
                int calculatedRate = (int) Math.round(
                        (amount / (double) monthlyIncomeBase) * 100.0);
                rate = Math.min(calculatedRate, 100);
            }

            expenseCategories.add(ExpenseCategoryDTO.builder()
                    .categoryId(categoryId++)
                    .categoryCode(code)
                    .categoryName(categoryName)
                    .amount(amount)
                    .rate(rate)
                    .build());
        }

        return UserExpenseDetailDTO.builder()
                .userId(userSummary.getUserId())
                .name(userSummary.getName())
                .expenseCategories(expenseCategories)
                .build();
    }

    private String resolveCategoryName(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) {
            return ExpenseCategory.ETC.getCategoryName();
        }
        try {
            return ExpenseCategory.valueOf(categoryCode.toUpperCase()).getCategoryName();
        } catch (IllegalArgumentException e) {
            return categoryCode;
        }
    }

    private boolean isWeddingCategory(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) {
            return false;
        }
        try {
            return ExpenseCategory.valueOf(categoryCode.toUpperCase()).isWedding();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    @Transactional
    public ExpenseFeedbackCreateResponseDTO createOrUpdateExpenseFeedback(
            Long userId, ExpenseFeedbackCreateRequestDTO requestDTO) {
        Long partnerId = monthlyMapper.selectConnectedPartnerId(userId);
        if (partnerId == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "연결된 파트너를 찾을 수 없습니다.");
        }

        ExpenseFeedbackEntityDTO feedback = ExpenseFeedbackEntityDTO.builder()
                .writerUserId(userId)
                .receiverUserId(partnerId)
                .year(requestDTO.getYear())
                .month(requestDTO.getMonth())
                .content(requestDTO.getContent())
                .build();

        monthlyMapper.upsertExpenseFeedback(feedback);

        ExpenseFeedbackCreateResponseDTO response =
                monthlyMapper.selectExpenseFeedbackById(feedback.getFeedbackId());
        if (response == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "등록된 피드백을 찾을 수 없습니다.");
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public SavingMissionResponseDTO getMonthlySavingMissions(Long userId, Integer year, Integer month) {
        List<SavingMissionDTO> missions = monthlyMapper.selectMonthlySavingMissions(userId, year, month);

        if (missions == null) {
            missions = Collections.emptyList();
        }

        YearMonth requestedYearMonth = YearMonth.of(year, month);
        if (!requestedYearMonth.equals(YearMonth.now())) {
            missions.forEach(mission -> mission.setIsSelectable(false));
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
    public void startSavingMission(Long userId, Long missionId) {
        int updatedRows = monthlyMapper.updateSavingMissionStatusToInProgress(userId, missionId);
        if (updatedRows == 0) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "현재 월의 도전하기 미션만 도전할 수 있습니다.");
        }
    }

    @Override
    @Transactional
    public int generateScheduledSavingMissions(YearMonth targetMonth) {
        List<Long> userIds = monthlyMapper.selectActiveUserIds();
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }

        int generatedUserCount = 0;
        for (Long userId : userIds) {
            boolean alreadyGenerated = monthlyMapper.existsSavingMissionByUserAndMonth(
                    userId, targetMonth.getYear(), targetMonth.getMonthValue());
            if (alreadyGenerated) {
                continue;
            }

            SavingMissionRecommendationRequestDTO request =
                    SavingMissionRecommendationRequestDTO.builder()
                            .userId(userId)
                            .targetYear(targetMonth.getYear())
                            .targetMonth(targetMonth.getMonthValue())
                            .build();
            ResponseEntity<ApiResponse<SavingMissionRecommendationResponseDTO>> response =
                    generateSavingMissionRecommendations(request);
            SavingMissionRecommendationResponseDTO data = response.getBody() != null
                    ? response.getBody().getData()
                    : null;
            if (data != null && data.getMissionCount() != null && data.getMissionCount() > 0) {
                generatedUserCount++;
            }
        }
        return generatedUserCount;
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<SavingMissionRecommendationResponseDTO>> generateSavingMissionRecommendations(SavingMissionRecommendationRequestDTO requestDTO) {
        Long userId = requestDTO.getUserId();
        Integer targetYear = requestDTO.getTargetYear();
        Integer targetMonth = requestDTO.getTargetMonth();

        if (targetYear == null || targetMonth == null || targetMonth < 1 || targetMonth > 12) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "대상 연도 및 월 정보가 올바르지 않습니다.");
        }

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

        List<CategoryExpenseStatisticsDTO> statisticsList =
                monthlyMapper.selectCategoryExpenseStatistics(analysisYear);
        Map<String, Long> averageAmountByCategory = new HashMap<>();
        if (statisticsList != null) {
            for (CategoryExpenseStatisticsDTO statistics : statisticsList) {
                if (statistics.getCategoryCode() != null && statistics.getAverageAmount() != null) {
                    averageAmountByCategory.put(
                            statistics.getCategoryCode().toUpperCase(),
                            statistics.getAverageAmount());
                }
            }
        }

        LocalDateTime targetCreatedAt = targetYM.atDay(1).atStartOfDay();

        List<RecommendedMissionDTO> recommendedMissions = new ArrayList<>();

        for (CategoryExpenseAnalysisDTO analysis : analysisList) {
            String categoryCode = analysis.getCategoryCode();
            Long totalAmount = analysis.getTotalAmount() != null ? analysis.getTotalAmount() : 0L;
            Integer txCount = analysis.getTxCount() != null ? analysis.getTxCount() : 0;
            Long averageAmount = categoryCode == null
                    ? null
                    : averageAmountByCategory.get(categoryCode.toUpperCase());

            if (averageAmount == null || totalAmount <= averageAmount || txCount <= 0) {
                continue;
            }

            GeneratedMissionInfoVO missionInfo = createStatisticsMission(
                    totalAmount,
                    txCount,
                    averageAmount);

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

    private GeneratedMissionInfoVO createStatisticsMission(
            Long totalAmount,
            Integer txCount,
            Long averageAmount) {
        int targetCount = BigDecimal.valueOf(averageAmount)
                .multiply(BigDecimal.valueOf(txCount))
                .divide(BigDecimal.valueOf(totalAmount), 0, RoundingMode.FLOOR)
                .intValue();
        targetCount = Math.max(0, Math.min(targetCount, txCount - 1));

        return GeneratedMissionInfoVO.builder()
                .title(String.format("월 %d회에서 %d회로 줄이면", txCount, targetCount))
                .expectedSavingAmount(totalAmount - averageAmount)
                .build();
    }

}
