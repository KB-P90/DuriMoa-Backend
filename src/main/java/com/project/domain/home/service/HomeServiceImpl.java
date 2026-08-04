package com.project.domain.home.service;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.home.dto.AssetSummaryResponseDTO;
import com.project.domain.home.dto.CoupleResponseDTO;
import com.project.domain.home.dto.ExpectedBudgetPreviewResponseDTO;
import com.project.domain.home.dto.ExpectedBudgetRegionResponseDTO;
import com.project.domain.home.dto.GoalProgressResponseDTO;
import com.project.domain.home.dto.GoalSummaryResponseDTO;
import com.project.domain.home.dto.HomeResponseDTO;
import com.project.domain.home.dto.MissionResponseDTO;
import com.project.domain.home.dto.SavingAlertResponseDTO;
import com.project.domain.home.dto.SavingMissionSummaryResponseDTO;
import com.project.domain.home.dto.SetupChecklistResponseDTO;
import com.project.domain.home.dto.SetupChecklistStepResponseDTO;
import com.project.domain.home.mapper.HomeMapper;
import com.project.domain.home.model.HomeRecord.HomeAssetSummaryRecord;
import com.project.domain.home.model.HomeRecord.HomeBudgetPreviewRecord;
import com.project.domain.home.model.HomeRecord.HomeChecklistEvidenceRecord;
import com.project.domain.home.model.HomeRecord.HomeCoupleRecord;
import com.project.domain.home.model.HomeRecord.HomeSavingMissionSummaryRecord;
import com.project.domain.home.model.HomeRecord.HomeUserRecord;
import com.project.domain.home.model.HomeSetupChecklistCalculator;
import com.project.domain.home.model.HomeSetupChecklistEvidence;
import com.project.domain.home.model.HomeSetupChecklistResult;
import com.project.domain.home.model.HomeSetupChecklistResult.StepResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private static final String MONTHLY_EXPENSE_NAVIGATION_MESSAGE = "월별지출관리 페이지로 이동";
    private static final String EXPECTED_BUDGET_TYPE = "균형형";
    private static final String EXPECTED_BUDGET_AMOUNT_BASIS = "MID_50";
    private static final List<String> EXPECTED_BUDGET_CATEGORIES =
            Collections.unmodifiableList(Arrays.asList("예식장", "스튜디오", "드레스", "메이크업"));

    // 홈 조회에 필요한 최소 DB 조회를 담당하는 Mapper입니다.
    private final HomeMapper homeMapper;

    // 실제 수행 근거를 5단계 체크리스트 상태로 변환하는 계산 객체입니다.
    private final HomeSetupChecklistCalculator checklistCalculator;

    // 인증 사용자의 홈 화면 응답 뼈대와 체크리스트를 조회합니다.
    @Override
    @Transactional(readOnly = true)
    public HomeResponseDTO getHome(Long authenticatedUserId) {
        validateAuthenticatedUser(authenticatedUserId);

        HomeUserRecord user = homeMapper.selectHomeUser(authenticatedUserId);
        if (user == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        HomeCoupleRecord couple = homeMapper.selectActiveCouple(authenticatedUserId);
        Boolean accountLinked = Boolean.TRUE.equals(homeMapper.selectAccountLinked(authenticatedUserId));
        HomeChecklistEvidenceRecord evidenceRecord = selectEvidence(authenticatedUserId, couple);
        HomeSetupChecklistEvidence evidence = buildChecklistEvidence(accountLinked, couple, evidenceRecord);
        HomeSetupChecklistResult checklistResult = checklistCalculator.calculate(evidence);
        Boolean setupCompleted = checklistResult.getCurrentStep() == null;
        HomeAssetSummaryRecord assetSummary = setupCompleted
                ? selectAssetSummary(authenticatedUserId, couple, evidenceRecord)
                : null;
        HomeSavingMissionSummaryRecord savingMissionSummary = setupCompleted
                ? selectSavingMissionSummary(authenticatedUserId)
                : null;
        List<HomeBudgetPreviewRecord> budgetPreviewRegions = setupCompleted
                ? Collections.emptyList()
                : selectExpectedBudgetPreviewRegions();

        return HomeResponseDTO.builder()
                .setupCompleted(setupCompleted)
                .couple(buildCoupleResponse(user, couple, evidenceRecord))
                .assetSummary(setupCompleted ? buildAssetSummary(assetSummary) : null)
                .savingAlert(setupCompleted ? buildSavingAlertPlaceholder() : null)
                .savingMissionSummary(setupCompleted ? buildSavingMissionSummary(savingMissionSummary) : null)
                .goalSummary(setupCompleted ? buildGoalSummary(evidenceRecord, assetSummary) : null)
                .setupChecklist(buildSetupChecklistResponse(checklistResult))
                .expectedBudgetPreview(setupCompleted
                        ? null
                        : buildExpectedBudgetPreview(budgetPreviewRegions))
                .build();
    }

    // 인증 사용자 ID가 홈 조회에 사용할 수 있는 값인지 확인합니다.
    private void validateAuthenticatedUser(Long authenticatedUserId) {
        if (authenticatedUserId == null || authenticatedUserId <= 0) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }

    // 활성 커플이 있을 때만 뒤 단계 완료 근거를 조회합니다.
    private HomeChecklistEvidenceRecord selectEvidence(Long authenticatedUserId, HomeCoupleRecord couple) {
        if (couple == null) {
            return null;
        }
        return homeMapper.selectChecklistEvidence(
                authenticatedUserId,
                couple.getCoupleId(),
                couple.getGroomId(),
                couple.getBrideId()
        );
    }

    // DB 조회 결과를 체크리스트 원시 완료 근거로 변환합니다.
    private HomeSetupChecklistEvidence buildChecklistEvidence(
            Boolean accountLinked,
            HomeCoupleRecord couple,
            HomeChecklistEvidenceRecord evidenceRecord
    ) {
        if (couple == null) {
            return HomeSetupChecklistEvidence.builder()
                    .accountLinked(Boolean.TRUE.equals(accountLinked))
                    .coupleConnected(false)
                    .weddingGoalCreated(false)
                    .mainPlanSelected(false)
                    .firstStepStarted(false)
                    .build();
        }

        Boolean mainPlanSelected = evidenceRecord != null
                && Boolean.TRUE.equals(evidenceRecord.getMainPlanSelected());
        return HomeSetupChecklistEvidence.builder()
                .accountLinked(Boolean.TRUE.equals(accountLinked))
                .coupleConnected(true)
                .weddingGoalCreated(evidenceRecord != null
                        && Boolean.TRUE.equals(evidenceRecord.getWeddingGoalCreated()))
                .mainPlanSelected(mainPlanSelected)
                .firstStepStarted(mainPlanSelected)
                .build();
    }

    // 사용자와 커플 연결 정보를 홈 상단 응답으로 변환합니다.
    private CoupleResponseDTO buildCoupleResponse(
            HomeUserRecord user,
            HomeCoupleRecord couple,
            HomeChecklistEvidenceRecord evidenceRecord
    ) {
        if (couple == null) {
            return CoupleResponseDTO.builder()
                    .userId(user.getUserId())
                    .userName(user.getUserName())
                    .userRole(user.getUserRole())
                    .build();
        }

        return CoupleResponseDTO.builder()
                .coupleId(couple.getCoupleId())
                .userId(couple.getUserId())
                .userName(couple.getUserName())
                .userRole(couple.getUserRole())
                .partnerUserId(couple.getPartnerUserId())
                .partnerName(couple.getPartnerName())
                .partnerRole(couple.getPartnerRole())
                .coupleStatus(couple.getCoupleStatus())
                .goalId(evidenceRecord == null ? null : evidenceRecord.getMainGoalId())
                .weddingDate(evidenceRecord == null ? null : evidenceRecord.getWeddingDate())
                .remainingMonths(null)
                .budgetPlanId(null)
                .build();
    }

    // 메인 홈에서 표시할 예산 달성률 계산용 금액을 조회합니다.
    private HomeAssetSummaryRecord selectAssetSummary(
            Long authenticatedUserId,
            HomeCoupleRecord couple,
            HomeChecklistEvidenceRecord evidenceRecord
    ) {
        if (couple == null || evidenceRecord == null || evidenceRecord.getMainGoalId() == null) {
            return null;
        }
        return homeMapper.selectAssetSummary(
                authenticatedUserId,
                couple.getGroomId(),
                couple.getBrideId(),
                evidenceRecord.getMainGoalId()
        );
    }

    // 체크리스트 미완료 홈에서 표시할 지역별 예상 예산 미리보기 데이터를 조회합니다.
    private List<HomeBudgetPreviewRecord> selectExpectedBudgetPreviewRegions() {
        List<HomeBudgetPreviewRecord> regions = homeMapper.selectExpectedBudgetPreviewRegions();
        return regions == null ? Collections.emptyList() : regions;
    }

    // 조회한 금액을 홈 자산 요약 응답으로 변환합니다.
    private AssetSummaryResponseDTO buildAssetSummary(HomeAssetSummaryRecord summaryRecord) {
        if (summaryRecord == null) {
            return AssetSummaryResponseDTO.builder().build();
        }

        return AssetSummaryResponseDTO.builder()
                .totalBudgetAmount(summaryRecord.getTotalBudgetAmount())
                .totalSavedAmount(nullToZero(summaryRecord.getTotalSavedAmount()))
                .weddingExpenseAmount(nullToZero(summaryRecord.getWeddingExpenseAmount()))
                .remainingWeddingFundAmount(nullToZero(summaryRecord.getRemainingWeddingFundAmount()))
                .baseDate(summaryRecord.getBaseDate())
                .build();
    }

    // TODO: 알림 제공자의 목표 변경 알림 조회 계약이 생기면 연결합니다.
    private SavingAlertResponseDTO buildSavingAlertPlaceholder() {
        return SavingAlertResponseDTO.builder().build();
    }

    // 홈에서 보여줄 현재 월 도전중 절약미션 요약을 조회합니다.
    private HomeSavingMissionSummaryRecord selectSavingMissionSummary(Long authenticatedUserId) {
        YearMonth currentYearMonth = YearMonth.now();
        return homeMapper.selectSavingMissionSummary(
                authenticatedUserId,
                currentYearMonth.getYear(),
                currentYearMonth.getMonthValue()
        );
    }

    // 도전중 절약미션 테이블 조회 결과를 홈 요약 응답으로 변환합니다.
    private SavingMissionSummaryResponseDTO buildSavingMissionSummary(
            HomeSavingMissionSummaryRecord summaryRecord
    ) {
        return SavingMissionSummaryResponseDTO.builder()
                .totalExpectedSavingAmount(summaryRecord == null
                        || summaryRecord.getTotalExpectedSavingAmount() == null
                        ? 0L
                        : summaryRecord.getTotalExpectedSavingAmount())
                .inProgressMissionCount(summaryRecord == null
                        || summaryRecord.getInProgressMissionCount() == null
                        ? 0
                        : summaryRecord.getInProgressMissionCount())
                .completedMissionCount(summaryRecord == null
                        || summaryRecord.getCompletedMissionCount() == null
                        ? 0
                        : summaryRecord.getCompletedMissionCount())
                .mission(buildMissionResponse(summaryRecord))
                .navigationMessage(MONTHLY_EXPENSE_NAVIGATION_MESSAGE)
                .build();
    }

    // 홈에 대표로 보여줄 절약미션이 없으면 null을 반환합니다.
    private MissionResponseDTO buildMissionResponse(HomeSavingMissionSummaryRecord summaryRecord) {
        if (summaryRecord == null || summaryRecord.getMissionId() == null) {
            return null;
        }

        return MissionResponseDTO.builder()
                .missionId(summaryRecord.getMissionId())
                .categoryCode(summaryRecord.getCategoryCode())
                .title(summaryRecord.getTitle())
                .status(summaryRecord.getStatus())
                .expectedSavingAmount(summaryRecord.getExpectedSavingAmount())
                .build();
    }

    // 목표 요약과 예산 달성률을 홈 응답으로 변환합니다.
    private GoalSummaryResponseDTO buildGoalSummary(
            HomeChecklistEvidenceRecord evidenceRecord,
            HomeAssetSummaryRecord assetSummary
    ) {
        Boolean hasJointGoal = evidenceRecord != null
                && Boolean.TRUE.equals(evidenceRecord.getWeddingGoalCreated());
        Long totalBudgetAmount = assetSummary == null ? null : assetSummary.getTotalBudgetAmount();
        Long totalSavedAmount = assetSummary == null ? null : assetSummary.getTotalSavedAmount();
        Long personalTargetAmount = assetSummary == null ? null : assetSummary.getPersonalTargetAmount();
        Long personalSavedAmount = assetSummary == null ? null : assetSummary.getPersonalSavedAmount();

        return GoalSummaryResponseDTO.builder()
                .jointGoal(GoalProgressResponseDTO.builder()
                        .goalId(hasJointGoal ? evidenceRecord.getMainGoalId() : null)
                        .status(hasJointGoal ? evidenceRecord.getMainGoalStatus() : null)
                        .targetAmount(totalBudgetAmount)
                        .currentAmount(nullToZero(totalSavedAmount))
                        .achievementRate(calculateAchievementRate(totalSavedAmount, totalBudgetAmount))
                        .setupRequired(!Boolean.TRUE.equals(hasJointGoal))
                        .build())
                .personalGoal(GoalProgressResponseDTO.builder()
                        .goalId(hasJointGoal ? evidenceRecord.getMainGoalId() : null)
                        .status(hasJointGoal ? evidenceRecord.getMainGoalStatus() : null)
                        .targetAmount(personalTargetAmount)
                        .currentAmount(nullToZero(personalSavedAmount))
                        .achievementRate(calculateAchievementRate(personalSavedAmount, personalTargetAmount))
                        .setupRequired(!Boolean.TRUE.equals(hasJointGoal))
                        .build())
                .build();
    }

    // 지역별 예상 예산 조회 결과를 미완료 홈 preview 응답으로 변환합니다.
    private ExpectedBudgetPreviewResponseDTO buildExpectedBudgetPreview(
            List<HomeBudgetPreviewRecord> previewRecords
    ) {
        List<HomeBudgetPreviewRecord> safePreviewRecords =
                previewRecords == null ? Collections.emptyList() : previewRecords;
        List<ExpectedBudgetRegionResponseDTO> regions = new ArrayList<>();
        for (int index = 0; index < safePreviewRecords.size(); index++) {
            HomeBudgetPreviewRecord record = safePreviewRecords.get(index);
            regions.add(ExpectedBudgetRegionResponseDTO.builder()
                    .region(record.getRegion())
                    .displayRegion(record.getDisplayRegion())
                    .balancedAmount(nullToZero(record.getBalancedAmount()))
                    .rank(index + 1)
                    .build());
        }

        return ExpectedBudgetPreviewResponseDTO.builder()
                .budgetType(EXPECTED_BUDGET_TYPE)
                .amountBasis(EXPECTED_BUDGET_AMOUNT_BASIS)
                .categories(EXPECTED_BUDGET_CATEGORIES)
                .regions(regions)
                .build();
    }

    // 계산 결과를 API 응답 DTO로 변환합니다.
    private SetupChecklistResponseDTO buildSetupChecklistResponse(HomeSetupChecklistResult checklistResult) {
        List<SetupChecklistStepResponseDTO> steps = checklistResult.getSteps().stream()
                .map(this::buildSetupChecklistStepResponse)
                .collect(Collectors.toList());

        return SetupChecklistResponseDTO.builder()
                .totalCount(checklistResult.getTotalCount())
                .completedCount(checklistResult.getCompletedCount())
                .currentStep(checklistResult.getCurrentStep())
                .steps(steps)
                .build();
    }

    // 단계별 계산 결과를 API 응답 DTO로 변환합니다.
    private SetupChecklistStepResponseDTO buildSetupChecklistStepResponse(StepResult stepResult) {
        return SetupChecklistStepResponseDTO.builder()
                .step(stepResult.getStep().getStep())
                .code(stepResult.getStep().getCode())
                .title(stepResult.getStep().getTitle())
                .completed(stepResult.getCompleted())
                .status(stepResult.getStatus().name())
                .build();
    }

    // 목표 금액 대비 현재 금액의 달성률을 정수 퍼센트로 계산합니다.
    private Integer calculateAchievementRate(Long currentAmount, Long targetAmount) {
        if (targetAmount == null) {
            return null;
        }
        if (targetAmount <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(nullToZero(currentAmount))
                .multiply(BigDecimal.valueOf(100L))
                .divide(BigDecimal.valueOf(targetAmount), 0, RoundingMode.DOWN)
                .intValue();
    }

    // 비어 있는 금액 값을 0으로 변환합니다.
    private Long nullToZero(Long amount) {
        return amount == null ? 0L : amount;
    }
}
