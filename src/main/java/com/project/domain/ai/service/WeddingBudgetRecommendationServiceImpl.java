package com.project.domain.ai.service;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.ai.domain.AiAnalysisScope;
import com.project.domain.ai.domain.AiBudgetType;
import com.project.domain.ai.dto.AiBudgetTypeModelResponseDTO;
import com.project.domain.ai.dto.WeddingBudgetItemsResponseDTO;
import com.project.domain.ai.dto.WeddingBudgetReasonDetailResponseDTO;
import com.project.domain.ai.dto.WeddingBudgetRecommendationRequestDTO;
import com.project.domain.ai.dto.WeddingBudgetRecommendationResponseDTO;
import com.project.domain.ai.mapper.AiRecommendationMapper;
import com.project.domain.ai.model.AiAnalysisMemberRecord;
import com.project.domain.ai.model.AiMonthlyFinanceRecord;
import com.project.domain.ai.model.AiWeddingBudgetSelectionContext;
import com.project.domain.goal.dto.GoalStatisticsResponseDTO;
import com.project.domain.goal.service.GoalStatisticsService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeddingBudgetRecommendationServiceImpl
        implements WeddingBudgetRecommendationService {

    private static final Integer ANALYSIS_MONTH_COUNT = 3;
    private static final Integer MINIMUM_SUFFICIENT_MONTH_COUNT = 3;
    private static final String DEFAULT_PROPOSAL_NAME = "AI 추천 시안";
    private static final BigDecimal RESERVE_RATE = new BigDecimal("0.07");
    private static final BigDecimal TOTAL_RATE = BigDecimal.ONE.add(RESERVE_RATE);
    private static final BigDecimal FLEX_MINIMUM_SAVING_RATE = new BigDecimal("0.25");
    private static final BigDecimal FLEX_MAXIMUM_EXPENSE_RATE = new BigDecimal("0.60");
    private static final Long ROUNDING_UNIT = 10_000L;
    private static final List<String> STATISTIC_CATEGORIES = List.of(
            "예식장",
            "스튜디오",
            "드레스",
            "메이크업"
    );

    private final AiRecommendationMapper aiRecommendationMapper;
    private final GoalStatisticsService goalStatisticsService;
    private final AiService aiService;

    @Override
    public WeddingBudgetRecommendationResponseDTO recommend(
            Long userId,
            WeddingBudgetRecommendationRequestDTO request
    ) {
        List<AiAnalysisMemberRecord> members =
                aiRecommendationMapper.selectAnalysisMembers(userId);
        if (members == null || members.isEmpty()) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        AnalysisPeriod analysisPeriod = resolveAnalysisPeriod(LocalDate.now());
        List<Long> userIds = members.stream()
                .map(AiAnalysisMemberRecord::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<AiMonthlyFinanceRecord> finances = aiRecommendationMapper.selectMonthlyFinances(
                userIds,
                analysisPeriod.startDate,
                analysisPeriod.endDate
        );
        if (finances == null) {
            finances = Collections.emptyList();
        }

        FinanceSummary financeSummary = summarizeFinances(finances);
        Integer monthsUntilWedding = calculateMonthsUntilWedding(request.getWeddingDate());
        Long weddingFund = calculateWeddingFund(members);
        Long affordableTotalBudget = calculateAffordableTotalBudget(
                weddingFund,
                financeSummary.sustainableMonthlySaving,
                monthsUntilWedding
        );

        List<CategoryStatistic> statistics = loadStatistics(
                request.getRegion(),
                request.getWeddingDate().getMonthValue()
        );
        Long lowerTotal = sumLower(statistics);
        Long medianTotal = sumMedian(statistics);
        Long upperTotal = sumUpper(statistics);
        Long affordableCoreBudget = calculateCoreBudget(affordableTotalBudget);
        Boolean dataSufficient = financeSummary.analysisMonths
                >= MINIMUM_SUFFICIENT_MONTH_COUNT;

        List<AiBudgetType> eligibleTypes = resolveEligibleTypes(
                dataSufficient,
                affordableCoreBudget,
                medianTotal,
                upperTotal,
                financeSummary
        );
        AiBudgetTypeModelResponseDTO selection = aiService.selectWeddingBudgetType(
                AiWeddingBudgetSelectionContext.builder()
                        .eligibleBudgetTypes(eligibleTypes)
                        .medianMonthlyIncome(financeSummary.medianIncome)
                        .medianMonthlyExpense(financeSummary.medianExpense)
                        .medianMonthlySaving(financeSummary.medianSaving)
                        .sustainableMonthlySaving(financeSummary.sustainableMonthlySaving)
                        .affordableTotalBudget(affordableTotalBudget)
                        .monthsUntilWedding(monthsUntilWedding)
                        .region(request.getRegion())
                        .build()
        );
        AiBudgetType selectedType = normalizeSelectedType(
                selection.getBudgetType(),
                eligibleTypes
        );
        Long targetCoreBudget = resolveTargetCoreBudget(
                selectedType,
                dataSufficient,
                affordableCoreBudget,
                lowerTotal,
                medianTotal,
                upperTotal
        );

        List<Long> allocatedAmounts = allocateAmounts(
                statistics,
                targetCoreBudget,
                lowerTotal,
                medianTotal,
                upperTotal
        );
        Long allocatedCoreBudget = allocatedAmounts.stream().mapToLong(Long::longValue).sum();
        Long reserve = roundDown(
                BigDecimal.valueOf(allocatedCoreBudget).multiply(RESERVE_RATE)
        );
        Long totalBudget = allocatedCoreBudget + reserve;

        return WeddingBudgetRecommendationResponseDTO.builder()
                .name(DEFAULT_PROPOSAL_NAME)
                .analysisScope(members.size() > 1
                        ? AiAnalysisScope.COUPLE
                        : AiAnalysisScope.PERSONAL)
                .analyzedMemberCount(members.size())
                .analysisMonths(financeSummary.analysisMonths)
                .dataSufficient(dataSufficient)
                .sustainableMonthlySaving(financeSummary.sustainableMonthlySaving)
                .affordableTotalBudget(affordableTotalBudget)
                .budgetType(selectedType.getCode())
                .items(WeddingBudgetItemsResponseDTO.builder()
                        .venue(allocatedAmounts.get(0))
                        .studio(allocatedAmounts.get(1))
                        .dress(allocatedAmounts.get(2))
                        .makeup(allocatedAmounts.get(3))
                        .reserve(reserve)
                        .build())
                .totalBudget(totalBudget)
                .groomRatio(50L)
                .brideRatio(50L)
                .reason(selection.getReason())
                .reasonDetails(buildReasonDetails(
                        members,
                        analysisPeriod,
                        financeSummary,
                        weddingFund,
                        monthsUntilWedding,
                        affordableTotalBudget,
                        request.getRegion(),
                        selectedType,
                        lowerTotal,
                        medianTotal,
                        upperTotal,
                        allocatedCoreBudget,
                        reserve,
                        selection.getReason()
                ))
                .warning(buildWarning(
                        dataSufficient,
                        affordableCoreBudget,
                        lowerTotal,
                        members
                ))
                .build();
    }

    AnalysisPeriod resolveAnalysisPeriod(LocalDate today) {
        YearMonth currentMonth = YearMonth.from(today);
        return new AnalysisPeriod(
                currentMonth.minusMonths(ANALYSIS_MONTH_COUNT).atDay(1),
                currentMonth.minusMonths(1).atEndOfMonth()
        );
    }

    private FinanceSummary summarizeFinances(List<AiMonthlyFinanceRecord> finances) {
        List<Long> incomes = new ArrayList<>();
        List<Long> expenses = new ArrayList<>();
        List<Long> savings = new ArrayList<>();
        List<Long> surpluses = new ArrayList<>();

        for (AiMonthlyFinanceRecord finance : finances) {
            Long income = nullToZero(finance.getIncome());
            Long expense = nullToZero(finance.getExpense());
            Long saving = nullToZero(finance.getSaving());
            incomes.add(income);
            expenses.add(expense);
            savings.add(saving);
            surpluses.add(Math.max(income - expense, 0L));
        }

        Long medianIncome = median(incomes);
        Long medianExpense = median(expenses);
        Long medianSaving = median(savings);
        Long medianSurplus = median(surpluses);
        Long sustainableMonthlySaving = medianSaving > 0
                ? Math.min(medianSurplus, medianSaving)
                : BigDecimal.valueOf(medianSurplus)
                        .multiply(new BigDecimal("0.50"))
                        .setScale(0, RoundingMode.DOWN)
                        .longValue();

        return new FinanceSummary(
                finances.size(),
                medianIncome,
                medianExpense,
                medianSaving,
                sustainableMonthlySaving
        );
    }

    private List<CategoryStatistic> loadStatistics(String region, Integer weddingMonth) {
        return STATISTIC_CATEGORIES.stream()
                .map(category -> goalStatisticsService.findGoalStatistics(
                        region,
                        category,
                        weddingMonth
                ))
                .map(this::toCategoryStatistic)
                .collect(Collectors.toList());
    }

    private CategoryStatistic toCategoryStatistic(GoalStatisticsResponseDTO statistic) {
        return new CategoryStatistic(
                nullToZero(statistic.getLower25()),
                nullToZero(statistic.getMedian()),
                nullToZero(statistic.getUpper25())
        );
    }

    private List<AiBudgetType> resolveEligibleTypes(
            Boolean dataSufficient,
            Long affordableCoreBudget,
            Long medianTotal,
            Long upperTotal,
            FinanceSummary financeSummary
    ) {
        if (!Boolean.TRUE.equals(dataSufficient) || affordableCoreBudget < medianTotal) {
            return List.of(AiBudgetType.SAVING);
        }

        List<AiBudgetType> eligibleTypes = new ArrayList<>(
                List.of(AiBudgetType.SAVING, AiBudgetType.BALANCED)
        );
        if (affordableCoreBudget >= upperTotal && isFlexEligible(financeSummary)) {
            eligibleTypes.add(AiBudgetType.FLEX);
        }
        return eligibleTypes;
    }

    private boolean isFlexEligible(FinanceSummary financeSummary) {
        if (financeSummary.medianIncome <= 0) {
            return false;
        }
        BigDecimal income = BigDecimal.valueOf(financeSummary.medianIncome);
        BigDecimal savingRate = BigDecimal.valueOf(financeSummary.medianSaving)
                .divide(income, 4, RoundingMode.DOWN);
        BigDecimal expenseRate = BigDecimal.valueOf(financeSummary.medianExpense)
                .divide(income, 4, RoundingMode.DOWN);
        return savingRate.compareTo(FLEX_MINIMUM_SAVING_RATE) >= 0
                && expenseRate.compareTo(FLEX_MAXIMUM_EXPENSE_RATE) <= 0;
    }

    private AiBudgetType normalizeSelectedType(
            AiBudgetType selectedType,
            List<AiBudgetType> eligibleTypes
    ) {
        if (selectedType != null && eligibleTypes.contains(selectedType)) {
            return selectedType;
        }
        if (eligibleTypes.contains(AiBudgetType.BALANCED)) {
            return AiBudgetType.BALANCED;
        }
        return AiBudgetType.SAVING;
    }

    private Long resolveTargetCoreBudget(
            AiBudgetType selectedType,
            Boolean dataSufficient,
            Long affordableCoreBudget,
            Long lowerTotal,
            Long medianTotal,
            Long upperTotal
    ) {
        if (!Boolean.TRUE.equals(dataSufficient)) {
            return lowerTotal;
        }
        switch (selectedType) {
            case FLEX:
                return Math.min(affordableCoreBudget, upperTotal);
            case BALANCED:
                return Math.min(affordableCoreBudget, medianTotal);
            case SAVING:
            default:
                if (affordableCoreBudget < medianTotal) {
                    return affordableCoreBudget;
                }
                return lowerTotal;
        }
    }

    private List<Long> allocateAmounts(
            List<CategoryStatistic> statistics,
            Long target,
            Long lowerTotal,
            Long medianTotal,
            Long upperTotal
    ) {
        List<Long> amounts = new ArrayList<>();
        if (target <= lowerTotal) {
            BigDecimal ratio = divide(target, lowerTotal);
            for (CategoryStatistic statistic : statistics) {
                amounts.add(roundDown(BigDecimal.valueOf(statistic.lower).multiply(ratio)));
            }
            return amounts;
        }

        if (target <= medianTotal) {
            BigDecimal ratio = divide(target - lowerTotal, medianTotal - lowerTotal);
            for (CategoryStatistic statistic : statistics) {
                amounts.add(interpolate(statistic.lower, statistic.median, ratio));
            }
            return amounts;
        }

        BigDecimal ratio = divide(target - medianTotal, upperTotal - medianTotal);
        for (CategoryStatistic statistic : statistics) {
            amounts.add(interpolate(statistic.median, statistic.upper, ratio));
        }
        return amounts;
    }

    private Long interpolate(Long start, Long end, BigDecimal ratio) {
        BigDecimal difference = BigDecimal.valueOf(end - start);
        return roundDown(BigDecimal.valueOf(start).add(difference.multiply(ratio)));
    }

    private BigDecimal divide(Long numerator, Long denominator) {
        if (denominator == null || denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(Math.max(numerator, 0L))
                .divide(BigDecimal.valueOf(denominator), 8, RoundingMode.DOWN)
                .min(BigDecimal.ONE);
    }

    private Long calculateAffordableTotalBudget(
            Long weddingFund,
            Long sustainableMonthlySaving,
            Integer monthsUntilWedding
    ) {
        return BigDecimal.valueOf(weddingFund)
                .add(BigDecimal.valueOf(sustainableMonthlySaving)
                        .multiply(BigDecimal.valueOf(monthsUntilWedding)))
                .min(BigDecimal.valueOf(Long.MAX_VALUE))
                .longValue();
    }

    private Long calculateCoreBudget(Long totalBudget) {
        return BigDecimal.valueOf(totalBudget)
                .divide(TOTAL_RATE, 0, RoundingMode.DOWN)
                .longValue();
    }

    private Long calculateWeddingFund(List<AiAnalysisMemberRecord> members) {
        return members.stream()
                .map(AiAnalysisMemberRecord::getWeddingFund)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
    }

    private Integer calculateMonthsUntilWedding(LocalDate weddingDate) {
        long months = ChronoUnit.MONTHS.between(
                YearMonth.now(),
                YearMonth.from(weddingDate)
        );
        return Math.toIntExact(Math.max(months, 1L));
    }

    private Long median(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return BigDecimal.valueOf(sorted.get(middle - 1))
                .add(BigDecimal.valueOf(sorted.get(middle)))
                .divide(BigDecimal.valueOf(2), 0, RoundingMode.DOWN)
                .longValue();
    }

    private Long roundDown(BigDecimal amount) {
        return amount.divide(
                        BigDecimal.valueOf(ROUNDING_UNIT),
                        0,
                        RoundingMode.DOWN
                )
                .multiply(BigDecimal.valueOf(ROUNDING_UNIT))
                .longValue();
    }

    private Long sumLower(List<CategoryStatistic> statistics) {
        return statistics.stream().mapToLong(value -> value.lower).sum();
    }

    private Long sumMedian(List<CategoryStatistic> statistics) {
        return statistics.stream().mapToLong(value -> value.median).sum();
    }

    private Long sumUpper(List<CategoryStatistic> statistics) {
        return statistics.stream().mapToLong(value -> value.upper).sum();
    }

    private String buildWarning(
            Boolean dataSufficient,
            Long affordableCoreBudget,
            Long lowerTotal,
            List<AiAnalysisMemberRecord> members
    ) {
        List<String> warnings = new ArrayList<>();
        if (!Boolean.TRUE.equals(dataSufficient)) {
            warnings.add("직전 3개월 데이터가 충분하지 않아 지역 알뜰형 통계를 중심으로 추천했어요.");
        } else if (affordableCoreBudget < lowerTotal) {
            warnings.add("준비 가능 예산이 지역 알뜰형 통계보다 낮아 일부 항목을 비례 조정했어요.");
        }
        if (members.stream().anyMatch(member -> member.getWeddingFund() == null)) {
            warnings.add("입력되지 않은 현재 결혼 자금은 0원으로 계산했어요.");
        }
        return warnings.isEmpty() ? null : String.join(" ", warnings);
    }

    private List<WeddingBudgetReasonDetailResponseDTO> buildReasonDetails(
            List<AiAnalysisMemberRecord> members,
            AnalysisPeriod analysisPeriod,
            FinanceSummary financeSummary,
            Long weddingFund,
            Integer monthsUntilWedding,
            Long affordableTotalBudget,
            String region,
            AiBudgetType selectedType,
            Long lowerTotal,
            Long medianTotal,
            Long upperTotal,
            Long allocatedCoreBudget,
            Long reserve,
            String selectionReason
    ) {
        String scope = members.size() > 1
                ? "커플 " + members.size() + "명"
                : "본인 1명";
        String period = formatDate(analysisPeriod.startDate)
                + "~"
                + formatDate(analysisPeriod.endDate);
        Long benchmark = resolveBudgetTypeBenchmark(
                selectedType,
                lowerTotal,
                medianTotal,
                upperTotal
        );

        return List.of(
                reasonDetail(
                        "분석 대상",
                        "직전 완료 3개월(" + period + ") 동안 " + scope
                                + "의 수입·지출·저축 내역 " + financeSummary.analysisMonths
                                + "개월치를 분석했어요."
                ),
                reasonDetail(
                        "월 저축 여력",
                        "월 중앙 수입 " + formatAmount(financeSummary.medianIncome)
                                + "과 지출 " + formatAmount(financeSummary.medianExpense)
                                + ", 실제 저축액 " + formatAmount(financeSummary.medianSaving)
                                + "을 비교해 매달 "
                                + formatAmount(financeSummary.sustainableMonthlySaving)
                                + "을 지속 가능한 저축액으로 계산했어요."
                ),
                reasonDetail(
                        "준비 가능 예산",
                        "현재 결혼 자금 " + formatAmount(weddingFund)
                                + "에 월 저축 가능액 "
                                + formatAmount(financeSummary.sustainableMonthlySaving)
                                + " × 결혼식까지 " + monthsUntilWedding
                                + "개월을 더해 최대 " + formatAmount(affordableTotalBudget)
                                + "을 준비 가능한 금액으로 봤어요."
                ),
                reasonDetail(
                        "시안 유형 선택",
                        region + "의 " + budgetTypeLabel(selectedType)
                                + " 핵심 항목 통계 " + formatAmount(benchmark)
                                + "과 준비 가능 금액을 비교했어요. "
                                + normalizeReason(selectionReason)
                ),
                reasonDetail(
                        "항목 배분과 예비비",
                        "예식 예정 월의 시기별 가중치를 예식장과 스드메에 각각 적용한 뒤, "
                                + "지역 통계의 항목별 비중으로 핵심 예산 "
                                + formatAmount(allocatedCoreBudget)
                                + "을 1만원 단위로 배분하고, 핵심 예산의 약 7%인 "
                                + formatAmount(reserve) + "을 예비비로 남겼어요."
                )
        );
    }

    private WeddingBudgetReasonDetailResponseDTO reasonDetail(
            String title,
            String description
    ) {
        return WeddingBudgetReasonDetailResponseDTO.builder()
                .title(title)
                .description(description)
                .build();
    }

    private Long resolveBudgetTypeBenchmark(
            AiBudgetType budgetType,
            Long lowerTotal,
            Long medianTotal,
            Long upperTotal
    ) {
        switch (budgetType) {
            case FLEX:
                return upperTotal;
            case BALANCED:
                return medianTotal;
            case SAVING:
            default:
                return lowerTotal;
        }
    }

    private String budgetTypeLabel(AiBudgetType budgetType) {
        switch (budgetType) {
            case FLEX:
                return "플렉스형(상위 25%)";
            case BALANCED:
                return "균형형(중앙값)";
            case SAVING:
            default:
                return "알뜰형(하위 25%)";
        }
    }

    private String formatDate(LocalDate date) {
        return date.getYear() + "."
                + String.format("%02d", date.getMonthValue()) + "."
                + String.format("%02d", date.getDayOfMonth());
    }

    private String formatAmount(Long amount) {
        return String.format("%,d원", nullToZero(amount));
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return "최근 소비 흐름과 저축 여력을 기준으로 이 유형을 선택했어요.";
        }
        return reason.trim();
    }

    private Long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    static final class AnalysisPeriod {
        private final LocalDate startDate;
        private final LocalDate endDate;

        private AnalysisPeriod(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        LocalDate getStartDate() {
            return startDate;
        }

        LocalDate getEndDate() {
            return endDate;
        }
    }

    private static final class FinanceSummary {
        private final Integer analysisMonths;
        private final Long medianIncome;
        private final Long medianExpense;
        private final Long medianSaving;
        private final Long sustainableMonthlySaving;

        private FinanceSummary(
                Integer analysisMonths,
                Long medianIncome,
                Long medianExpense,
                Long medianSaving,
                Long sustainableMonthlySaving
        ) {
            this.analysisMonths = analysisMonths;
            this.medianIncome = medianIncome;
            this.medianExpense = medianExpense;
            this.medianSaving = medianSaving;
            this.sustainableMonthlySaving = sustainableMonthlySaving;
        }
    }

    private static final class CategoryStatistic {
        private final Long lower;
        private final Long median;
        private final Long upper;

        private CategoryStatistic(Long lower, Long median, Long upper) {
            this.lower = lower;
            this.median = median;
            this.upper = upper;
        }
    }
}
