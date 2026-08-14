package com.project.domain.home.service;

import com.project.common.enums.CoupleStatus;
import com.project.common.enums.ExpenseCategory;
import com.project.common.enums.MissionStatus;
import com.project.common.enums.RawRecordCategory;
import com.project.common.enums.RecordType;
import com.project.common.enums.SavingCategory;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.goal.domain.GoalStatus;
import com.project.domain.home.dto.AssetSummaryResponseDTO;
import com.project.domain.home.dto.CoupleResponseDTO;
import com.project.domain.home.dto.ExpectedBudgetPreviewResponseDTO;
import com.project.domain.home.dto.ExpectedBudgetRegionResponseDTO;
import com.project.domain.home.dto.GoalProgressResponseDTO;
import com.project.domain.home.dto.GoalSummaryResponseDTO;
import com.project.domain.home.dto.HomeResponseDTO;
import com.project.domain.home.dto.MissionResponseDTO;
import com.project.domain.home.dto.NotificationSummaryResponseDTO;
import com.project.domain.home.dto.SavingAlertResponseDTO;
import com.project.domain.home.dto.SavingMissionSummaryResponseDTO;
import com.project.domain.home.dto.SetupChecklistResponseDTO;
import com.project.domain.home.dto.SetupChecklistStepResponseDTO;
import com.project.domain.home.mapper.HomeMapper;
import com.project.domain.home.model.HomeRecord.HomeAssetRecord;
import com.project.domain.home.model.HomeRecord.HomeAssetSummaryRecord;
import com.project.domain.home.model.HomeRecord.HomeBudgetPreviewRecord;
import com.project.domain.home.model.HomeRecord.HomeChecklistEvidenceRecord;
import com.project.domain.home.model.HomeRecord.HomeCoupleRecord;
import com.project.domain.home.model.HomeRecord.HomeMemberAssetRecord;
import com.project.domain.home.model.HomeRecord.HomeSavingAlertRecord;
import com.project.domain.home.model.HomeRecord.HomeSavingMissionSummaryRecord;
import com.project.domain.home.model.HomeRecord.HomeUserRecord;
import com.project.domain.home.model.HomeSetupChecklistCalculator;
import com.project.domain.home.model.HomeSetupChecklistEvidence;
import com.project.domain.home.model.HomeSetupChecklistResult;
import com.project.domain.home.model.HomeSetupChecklistResult.StepResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
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
    private static final List<String> EXPECTED_BUDGET_REGIONS =
            Collections.unmodifiableList(Arrays.asList(
                    "서울(강남)", "경기도", "인천", "부산", "대구", "광주", "대전"));

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

        HomeCoupleRecord couple = homeMapper.selectActiveCouple(
                authenticatedUserId,
                CoupleStatus.CONNECTED.getStatus()
        );
        Boolean accountLinked = Boolean.TRUE.equals(homeMapper.selectAccountLinked(authenticatedUserId));
        HomeChecklistEvidenceRecord evidenceRecord = selectEvidence(authenticatedUserId, couple);
        HomeSetupChecklistEvidence evidence = buildChecklistEvidence(accountLinked, couple, evidenceRecord);
        HomeSetupChecklistResult checklistResult = checklistCalculator.calculate(evidence);
        Boolean setupCompleted = checklistResult.getCurrentStep() == null;
        HomeAssetSummaryRecord assetSummary = setupCompleted
                ? selectAssetSummary(authenticatedUserId, couple, evidenceRecord)
                : null;
        HomeSavingAlertRecord savingAlert = setupCompleted
                ? homeMapper.selectLatestSavingAlert(authenticatedUserId)
                : null;
        Integer unreadNotificationCount = homeMapper.countUnreadNotifications(authenticatedUserId);
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
                .savingAlert(setupCompleted ? buildSavingAlert(savingAlert) : null)
                .notificationSummary(buildNotificationSummary(unreadNotificationCount))
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
                couple.getBrideId(),
                GoalStatus.NORMAL.name(),
                GoalStatus.PENDING.name(),
                GoalStatus.CONFIRMED.name()
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
                    .build();
        }

        return HomeSetupChecklistEvidence.builder()
                .accountLinked(Boolean.TRUE.equals(accountLinked))
                .coupleConnected(true)
                .weddingGoalCreated(evidenceRecord != null
                        && Boolean.TRUE.equals(evidenceRecord.getWeddingGoalCreated()))
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
                .remainingMonths(calculateRemainingMonths(evidenceRecord))
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
        HomeAssetSummaryRecord summaryRecord = homeMapper.selectAssetSummary(
                authenticatedUserId,
                couple.getGroomId(),
                couple.getBrideId(),
                evidenceRecord.getMainGoalId()
        );
        if (summaryRecord == null) {
            return null;
        }

        List<HomeAssetRecord> assetRecords = homeMapper.selectAssetRecords(
                couple.getGroomId(),
                couple.getBrideId()
        );
        List<HomeMemberAssetRecord> memberAssets = homeMapper.selectHomeMemberAssets(
                couple.getGroomId(),
                couple.getBrideId()
        );
        applyAssetAmounts(authenticatedUserId, summaryRecord, assetRecords, memberAssets);
        return summaryRecord;
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
                .remainingWeddingFundAmount(calculateRemainingWeddingFundAmount(summaryRecord))
                .baseDate(summaryRecord.getBaseDate())
                .build();
    }

    // 최신 알림 조회 결과를 홈 알림 배너 응답으로 변환합니다.
    private SavingAlertResponseDTO buildSavingAlert(HomeSavingAlertRecord savingAlert) {
        if (savingAlert == null) {
            return null;
        }

        return SavingAlertResponseDTO.builder()
                .alertType(savingAlert.getAlertType())
                .title(savingAlert.getTitle())
                .message(savingAlert.getMessage())
                .createdAt(savingAlert.getCreatedAt())
                .build();
    }

    // 홈 알림 버튼에 필요한 읽지 않은 알림 상태를 응답으로 변환합니다.
    private NotificationSummaryResponseDTO buildNotificationSummary(Integer unreadNotificationCount) {
        Integer safeUnreadCount = unreadNotificationCount == null ? 0 : unreadNotificationCount;
        return NotificationSummaryResponseDTO.builder()
                .unreadCount(safeUnreadCount)
                .hasUnread(safeUnreadCount > 0)
                .build();
    }

    // 홈에서 보여줄 현재 월 도전중 절약미션 요약을 조회합니다.
    private HomeSavingMissionSummaryRecord selectSavingMissionSummary(Long authenticatedUserId) {
        YearMonth currentYearMonth = YearMonth.now();
        return homeMapper.selectSavingMissionSummary(
                authenticatedUserId,
                currentYearMonth.getYear(),
                currentYearMonth.getMonthValue(),
                MissionStatus.CHALLENGE.getStatus(),
                MissionStatus.IN_PROGRESS.getStatus(),
                MissionStatus.COMPLETED.getStatus()
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

    // 거래 원본과 자산 공개 정보를 홈 자산 요약 금액으로 합산합니다.
    private void applyAssetAmounts(
            Long authenticatedUserId,
            HomeAssetSummaryRecord summaryRecord,
            List<HomeAssetRecord> assetRecords,
            List<HomeMemberAssetRecord> memberAssets
    ) {
        AssetAmounts weddingFundAmounts = calculateWeddingFundAmounts(authenticatedUserId, memberAssets);
        for (HomeAssetRecord assetRecord : nullToEmpty(assetRecords)) {
            Long amount = nullToZero(assetRecord.getAmount());
            if (isWeddingSavingRecord(assetRecord)) {
                weddingFundAmounts.totalSavedAmount += amount;
                if (authenticatedUserId.equals(assetRecord.getUserId())) {
                    weddingFundAmounts.personalSavedAmount += amount;
                }
            }
            if (isWeddingExpenseRecord(assetRecord)) {
                weddingFundAmounts.weddingExpenseAmount += amount;
            }
        }

        summaryRecord.setTotalSavedAmount(weddingFundAmounts.totalSavedAmount);
        summaryRecord.setPersonalSavedAmount(weddingFundAmounts.personalSavedAmount);
        summaryRecord.setWeddingExpenseAmount(weddingFundAmounts.weddingExpenseAmount);
    }

    // 사용자가 결혼자금으로 포함하겠다고 입력한 현재 보유 금액을 계산합니다.
    private AssetAmounts calculateWeddingFundAmounts(
            Long authenticatedUserId,
            List<HomeMemberAssetRecord> memberAssets
    ) {
        AssetAmounts amounts = new AssetAmounts();
        for (HomeMemberAssetRecord memberAsset : nullToEmpty(memberAssets)) {
            Long weddingFund = nullToZero(memberAsset.getWeddingFund());
            amounts.totalSavedAmount += weddingFund;
            if (authenticatedUserId.equals(memberAsset.getUserId())) {
                amounts.personalSavedAmount += weddingFund;
            }
        }
        return amounts;
    }

    // 거래가 결혼 저축 금액으로 집계되어야 하는지 판단합니다.
    private Boolean isWeddingSavingRecord(HomeAssetRecord assetRecord) {
        return assetRecord != null
                && RecordType.SAVING.name().equalsIgnoreCase(assetRecord.getRecordType())
                && isWeddingSavingCategory(assetRecord.getCategory());
    }

    // 거래가 결혼 준비 지출로 집계되어야 하는지 판단합니다.
    private Boolean isWeddingExpenseRecord(HomeAssetRecord assetRecord) {
        return assetRecord != null
                && RecordType.EXPENSE.name().equalsIgnoreCase(assetRecord.getRecordType())
                && isWeddingExpenseCategory(assetRecord.getCategory());
    }

    // 저축 카테고리가 결혼 저축인지 공통 enum 기준으로 판단합니다.
    private Boolean isWeddingSavingCategory(String category) {
        String trimmedCategory = trim(category);
        if (trimmedCategory == null) {
            return false;
        }
        for (String weddingCode : SavingCategory.getWeddingCodes()) {
            SavingCategory savingCategory = SavingCategory.valueOf(weddingCode);
            if (matchesCategory(trimmedCategory, weddingCode, savingCategory.getCategoryName())) {
                return true;
            }
        }
        return RawRecordCategory.WEDDING.matches(trimmedCategory);
    }

    // 지출 카테고리가 결혼 준비 지출인지 공통 enum 기준으로 판단합니다.
    private Boolean isWeddingExpenseCategory(String category) {
        String trimmedCategory = trim(category);
        if (trimmedCategory == null) {
            return false;
        }
        for (String weddingCode : ExpenseCategory.getWeddingCodes()) {
            ExpenseCategory expenseCategory = ExpenseCategory.valueOf(weddingCode);
            if (matchesCategory(trimmedCategory, weddingCode, expenseCategory.getCategoryName())) {
                return true;
            }
        }
        return RawRecordCategory.WEDDING.matches(trimmedCategory);
    }

    // 카테고리 코드와 표시명을 모두 허용합니다.
    private Boolean matchesCategory(String category, String categoryCode, String categoryName) {
        return category.equalsIgnoreCase(categoryCode) || category.equals(categoryName);
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
        List<ExpenseCategory> expectedBudgetCategories = resolveExpectedBudgetCategories(safePreviewRecords);
        List<ExpectedBudgetRegionResponseDTO> regions = new ArrayList<>();
        for (String region : EXPECTED_BUDGET_REGIONS) {
            Long balancedAmount = calculateExpectedBudgetRegionAmount(
                    region,
                    expectedBudgetCategories,
                    safePreviewRecords
            );
            if (balancedAmount == null) {
                continue;
            }
            regions.add(ExpectedBudgetRegionResponseDTO.builder()
                    .region(region)
                    .displayRegion(resolveDisplayRegion(region))
                    .balancedAmount(balancedAmount)
                    .rank(regions.size() + 1)
                    .build());
        }

        return ExpectedBudgetPreviewResponseDTO.builder()
                .budgetType(EXPECTED_BUDGET_TYPE)
                .amountBasis(EXPECTED_BUDGET_AMOUNT_BASIS)
                .categories(buildExpectedBudgetCategories(expectedBudgetCategories))
                .regions(regions)
                .build();
    }

    // 공통 enum에 정의된 결혼 예상 예산 카테고리 표시명을 응답에 사용합니다.
    private List<String> buildExpectedBudgetCategories(
            List<ExpenseCategory> expectedBudgetCategories
    ) {
        return expectedBudgetCategories.stream()
                .map(ExpenseCategory::getCategoryName)
                .collect(Collectors.toList());
    }

    // 통계 테이블에 실제 데이터가 있는 결혼 카테고리만 공통 enum 코드 목록에서 선택합니다.
    private List<ExpenseCategory> resolveExpectedBudgetCategories(
            List<HomeBudgetPreviewRecord> previewRecords
    ) {
        return ExpenseCategory.getWeddingCodes().stream()
                .map(ExpenseCategory::valueOf)
                .filter(category -> existsBudgetPreviewCategory(category, previewRecords))
                .collect(Collectors.toList());
    }

    private Boolean existsBudgetPreviewCategory(
            ExpenseCategory category,
            List<HomeBudgetPreviewRecord> previewRecords
    ) {
        for (HomeBudgetPreviewRecord record : previewRecords) {
            String recordCategory = trim(record.getCategory());
            if (recordCategory != null
                    && matchesCategory(recordCategory, category.name(), category.getCategoryName())) {
                return true;
            }
        }
        return false;
    }

    // 지역별 예상 예산 합계를 서비스 로직에서 계산합니다.
    private Long calculateExpectedBudgetRegionAmount(
            String region,
            List<ExpenseCategory> expectedBudgetCategories,
            List<HomeBudgetPreviewRecord> previewRecords
    ) {
        if (expectedBudgetCategories.isEmpty()) {
            return null;
        }
        Long totalAmount = 0L;
        for (ExpenseCategory category : expectedBudgetCategories) {
            Long categoryAmount = calculateExpectedBudgetCategoryAmount(region, category, previewRecords);
            if (categoryAmount == null) {
                return null;
            }
            totalAmount += categoryAmount;
        }
        return totalAmount;
    }

    // 지역·카테고리 단위 예상 예산 금액을 합산합니다.
    private Long calculateExpectedBudgetCategoryAmount(
            String region,
            ExpenseCategory category,
            List<HomeBudgetPreviewRecord> previewRecords
    ) {
        Long categoryAmount = 0L;
        Boolean categoryFound = false;
        for (HomeBudgetPreviewRecord record : previewRecords) {
            String recordCategory = trim(record.getCategory());
            if (region.equals(trim(record.getRegion()))
                    && recordCategory != null
                    && matchesCategory(recordCategory, category.name(), category.getCategoryName())) {
                categoryAmount += nullToZero(record.getBalancedAmount());
                categoryFound = true;
            }
        }
        return Boolean.TRUE.equals(categoryFound) ? categoryAmount : null;
    }

    // 홈 차트에 표시할 짧은 지역명을 계산합니다.
    private String resolveDisplayRegion(String region) {
        if ("서울(강남)".equals(region) || "서울(강남외)".equals(region)) {
            return "서울";
        }
        if ("경기도".equals(region)) {
            return "경기";
        }
        return region;
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

    // 결혼 예정일까지 남은 개월 수를 계산합니다.
    private Integer calculateRemainingMonths(HomeChecklistEvidenceRecord evidenceRecord) {
        if (evidenceRecord == null || evidenceRecord.getWeddingDate() == null) {
            return null;
        }
        try {
            LocalDate weddingDate = LocalDate.parse(evidenceRecord.getWeddingDate());
            LocalDate today = LocalDate.now();
            if (!weddingDate.isAfter(today)) {
                return 0;
            }
            return Math.toIntExact(ChronoUnit.MONTHS.between(
                    YearMonth.from(today),
                    YearMonth.from(weddingDate)
            ));
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    // 총 예산에서 현재 모은 금액을 차감해 남은 결혼 자금을 계산합니다.
    private Long calculateRemainingWeddingFundAmount(HomeAssetSummaryRecord summaryRecord) {
        if (summaryRecord.getRemainingWeddingFundAmount() != null) {
            return nullToZero(summaryRecord.getRemainingWeddingFundAmount());
        }
        Long totalBudgetAmount = summaryRecord.getTotalBudgetAmount();
        if (totalBudgetAmount == null) {
            return 0L;
        }
        return Math.max(totalBudgetAmount - nullToZero(summaryRecord.getTotalSavedAmount()), 0L);
    }

    // 비어 있는 금액 값을 0으로 변환합니다.
    private Long nullToZero(Long amount) {
        return amount == null ? 0L : amount;
    }

    private String trim(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private static class AssetAmounts {

        private Long totalSavedAmount = 0L;
        private Long personalSavedAmount = 0L;
        private Long weddingExpenseAmount = 0L;
    }
}
