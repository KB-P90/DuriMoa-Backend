package com.project.domain.ai.service;

import com.project.common.enums.ExpenseCategory;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.ai.domain.SpendingTrendUnit;
import com.project.domain.ai.dto.AiSpendingReportSummaryModelResponseDTO;
import com.project.domain.ai.dto.SpendingReportCategoryResponseDTO;
import com.project.domain.ai.dto.SpendingReportResponseDTO;
import com.project.domain.ai.dto.SpendingReportTrendResponseDTO;
import com.project.domain.ai.dto.SpendingReportWeekdayAnalysisResponseDTO;
import com.project.domain.ai.dto.SpendingReportWeekdayResponseDTO;
import com.project.domain.ai.mapper.AiSpendingReportMapper;
import com.project.domain.ai.model.AiCategorySpendingRecord;
import com.project.domain.ai.model.AiDailySpendingRecord;
import com.project.domain.ai.model.AiSpendingReportSummaryContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SpendingReportServiceImpl implements SpendingReportService {

    private static final Integer MINIMUM_TRANSACTION_COUNT = 5;
    private static final Integer MINIMUM_ANALYSIS_DAYS = 14;
    private static final Integer WEEKLY_TREND_MAX_DAYS = 90;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final DateTimeFormatter MONTH_KEY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final AiSpendingReportMapper spendingReportMapper;
    private final AiService aiService;

    @Override
    @Transactional(readOnly = true)
    public SpendingReportResponseDTO generate(Long userId) {
        return generate(userId, LocalDateTime.now());
    }

    SpendingReportResponseDTO generate(Long userId, LocalDateTime requestedAt) {
        LocalDateTime createdAt = spendingReportMapper.selectUserCreatedAt(userId);
        if (createdAt == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "사용자 정보를 찾을 수 없습니다.");
        }

        LocalDate endDate = requestedAt.toLocalDate();
        LocalDate startDate = createdAt.toLocalDate().isAfter(endDate)
                ? endDate
                : createdAt.toLocalDate();
        Integer analysisDays = Math.toIntExact(ChronoUnit.DAYS.between(startDate, endDate) + 1);

        List<AiDailySpendingRecord> dailyRecords = spendingReportMapper.selectDailySpending(
                userId,
                startDate,
                endDate
        );
        List<AiCategorySpendingRecord> categoryRecords = spendingReportMapper
                .selectCategorySpending(userId, startDate, endDate);
        AiCategorySpendingRecord topWeekendCategory = spendingReportMapper
                .selectTopWeekendCategory(userId, startDate, endDate);

        Long totalExpense = dailyRecords.stream()
                .map(AiDailySpendingRecord::getAmount)
                .map(this::orZero)
                .reduce(0L, Long::sum);
        Integer transactionCount = dailyRecords.stream()
                .map(AiDailySpendingRecord::getTransactionCount)
                .map(this::orZero)
                .reduce(0, Integer::sum);
        Long averageMonthlyExpense = divide(
                BigDecimal.valueOf(totalExpense).multiply(BigDecimal.valueOf(30)),
                BigDecimal.valueOf(analysisDays)
        ).longValue();
        Boolean dataSufficient = transactionCount >= MINIMUM_TRANSACTION_COUNT
                && analysisDays >= MINIMUM_ANALYSIS_DAYS;

        SpendingTrendUnit trendUnit = analysisDays < WEEKLY_TREND_MAX_DAYS
                ? SpendingTrendUnit.WEEKLY
                : SpendingTrendUnit.MONTHLY;
        List<SpendingReportTrendResponseDTO> trend = buildTrend(
                dailyRecords,
                startDate,
                endDate,
                trendUnit
        );
        BigDecimal recentTrendRate = calculateRecentTrendRate(trend);
        List<SpendingReportCategoryResponseDTO> categories = buildCategories(
                categoryRecords,
                totalExpense
        );
        SpendingReportCategoryResponseDTO topCategory = categories.stream().findFirst().orElse(null);
        SpendingReportWeekdayAnalysisResponseDTO weekdayAnalysis = buildWeekdayAnalysis(
                dailyRecords,
                startDate,
                endDate,
                topWeekendCategory
        );

        String warning = dataSufficient
                ? null
                : "분석 기간이 14일 미만이거나 지출 내역이 5건 미만이라 해석이 제한적이에요.";
        AiSpendingReportSummaryContext summaryContext = AiSpendingReportSummaryContext.builder()
                .totalExpense(totalExpense)
                .averageMonthlyExpense(averageMonthlyExpense)
                .transactionCount(transactionCount)
                .topCategoryName(topCategory == null ? null : topCategory.getName())
                .topCategoryRatio(topCategory == null ? null : topCategory.getRatio())
                .recentTrendRate(recentTrendRate)
                .peakDayName(weekdayAnalysis.getPeakDayName())
                .weekendToWeekdayRatio(weekdayAnalysis.getWeekendToWeekdayRatio())
                .topWeekendCategoryName(weekdayAnalysis.getTopWeekendCategoryName())
                .build();
        String summary = dataSufficient
                ? resolveSummary(aiService.summarizeSpendingReport(summaryContext), summaryContext)
                : buildFallbackSummary(summaryContext);

        return SpendingReportResponseDTO.builder()
                .analysisStartDate(startDate)
                .analysisEndDate(endDate)
                .dataStartDate(dailyRecords.stream()
                        .map(AiDailySpendingRecord::getRecordDate)
                        .min(LocalDate::compareTo)
                        .orElse(null))
                .generatedAt(requestedAt)
                .analysisDays(analysisDays)
                .dataSufficient(dataSufficient)
                .totalExpense(totalExpense)
                .averageMonthlyExpense(averageMonthlyExpense)
                .transactionCount(transactionCount)
                .topCategoryName(topCategory == null ? null : topCategory.getName())
                .topCategoryRatio(topCategory == null ? BigDecimal.ZERO : topCategory.getRatio())
                .recentTrendRate(recentTrendRate)
                .trendUnit(trendUnit)
                .trend(trend)
                .categories(categories)
                .weekdayAnalysis(weekdayAnalysis)
                .summary(summary)
                .warning(warning)
                .build();
    }

    private List<SpendingReportTrendResponseDTO> buildTrend(
            List<AiDailySpendingRecord> dailyRecords,
            LocalDate startDate,
            LocalDate endDate,
            SpendingTrendUnit unit
    ) {
        Map<String, AiDailySpendingRecord> recordsByDate = dailyRecords.stream()
                .collect(Collectors.toMap(
                        record -> record.getRecordDate().toString(),
                        Function.identity()
                ));
        List<SpendingReportTrendResponseDTO> result = new ArrayList<>();

        if (unit == SpendingTrendUnit.WEEKLY) {
            LocalDate bucketStart = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            while (!bucketStart.isAfter(endDate)) {
                LocalDate bucketEnd = bucketStart.plusDays(6);
                result.add(buildTrendBucket(
                        recordsByDate,
                        bucketStart,
                        bucketEnd,
                        startDate,
                        endDate,
                        bucketStart.toString(),
                        bucketStart.getMonthValue() + "/" + bucketStart.getDayOfMonth()
                ));
                bucketStart = bucketStart.plusWeeks(1);
            }
            return result;
        }

        YearMonth bucketMonth = YearMonth.from(startDate);
        YearMonth finalMonth = YearMonth.from(endDate);
        while (!bucketMonth.isAfter(finalMonth)) {
            LocalDate bucketStart = bucketMonth.atDay(1);
            LocalDate bucketEnd = bucketMonth.atEndOfMonth();
            result.add(buildTrendBucket(
                    recordsByDate,
                    bucketStart,
                    bucketEnd,
                    startDate,
                    endDate,
                    bucketMonth.format(MONTH_KEY_FORMAT),
                    bucketMonth.getYear() + "." + bucketMonth.getMonthValue()
            ));
            bucketMonth = bucketMonth.plusMonths(1);
        }
        return result;
    }

    private SpendingReportTrendResponseDTO buildTrendBucket(
            Map<String, AiDailySpendingRecord> recordsByDate,
            LocalDate bucketStart,
            LocalDate bucketEnd,
            LocalDate analysisStart,
            LocalDate analysisEnd,
            String key,
            String label
    ) {
        LocalDate effectiveStart = bucketStart.isBefore(analysisStart) ? analysisStart : bucketStart;
        LocalDate effectiveEnd = bucketEnd.isAfter(analysisEnd) ? analysisEnd : bucketEnd;
        Long amount = 0L;
        Integer transactionCount = 0;
        LocalDate date = effectiveStart;
        while (!date.isAfter(effectiveEnd)) {
            AiDailySpendingRecord record = recordsByDate.get(date.toString());
            if (record != null) {
                amount += orZero(record.getAmount());
                transactionCount += orZero(record.getTransactionCount());
            }
            date = date.plusDays(1);
        }
        return SpendingReportTrendResponseDTO.builder()
                .key(key)
                .label(label)
                .startDate(effectiveStart)
                .endDate(effectiveEnd)
                .amount(amount)
                .transactionCount(transactionCount)
                .partialPeriod(!effectiveStart.equals(bucketStart) || !effectiveEnd.equals(bucketEnd))
                .build();
    }

    private BigDecimal calculateRecentTrendRate(List<SpendingReportTrendResponseDTO> trend) {
        List<SpendingReportTrendResponseDTO> completePeriods = trend.stream()
                .filter(period -> !period.getPartialPeriod())
                .collect(Collectors.toList());
        if (completePeriods.size() < 2) {
            return null;
        }
        Long previousAmount = completePeriods.get(completePeriods.size() - 2).getAmount();
        Long recentAmount = completePeriods.get(completePeriods.size() - 1).getAmount();
        if (previousAmount == 0L) {
            return null;
        }
        return divide(
                BigDecimal.valueOf(recentAmount - previousAmount).multiply(ONE_HUNDRED),
                BigDecimal.valueOf(previousAmount)
        );
    }

    private List<SpendingReportCategoryResponseDTO> buildCategories(
            List<AiCategorySpendingRecord> categoryRecords,
            Long totalExpense
    ) {
        return categoryRecords.stream()
                .map(record -> SpendingReportCategoryResponseDTO.builder()
                        .code(record.getCategory())
                        .name(resolveCategoryName(record.getCategory()))
                        .amount(orZero(record.getAmount()))
                        .ratio(totalExpense == 0L
                                ? BigDecimal.ZERO
                                : divide(
                                        BigDecimal.valueOf(orZero(record.getAmount()))
                                                .multiply(ONE_HUNDRED),
                                        BigDecimal.valueOf(totalExpense)
                                ))
                        .transactionCount(orZero(record.getTransactionCount()))
                        .build())
                .sorted(Comparator.comparing(
                        SpendingReportCategoryResponseDTO::getAmount,
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    private SpendingReportWeekdayAnalysisResponseDTO buildWeekdayAnalysis(
            List<AiDailySpendingRecord> dailyRecords,
            LocalDate startDate,
            LocalDate endDate,
            AiCategorySpendingRecord topWeekendCategory
    ) {
        Map<DayOfWeek, Long> totalByDay = new EnumMap<>(DayOfWeek.class);
        Map<DayOfWeek, Integer> calendarDaysByDay = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            totalByDay.put(dayOfWeek, 0L);
            calendarDaysByDay.put(dayOfWeek, 0);
        }
        dailyRecords.forEach(record -> totalByDay.merge(
                record.getRecordDate().getDayOfWeek(),
                orZero(record.getAmount()),
                Long::sum
        ));
        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            calendarDaysByDay.merge(date.getDayOfWeek(), 1, Integer::sum);
            date = date.plusDays(1);
        }

        Map<DayOfWeek, Long> averageByDay = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            averageByDay.put(dayOfWeek, divide(
                    BigDecimal.valueOf(totalByDay.get(dayOfWeek)),
                    BigDecimal.valueOf(calendarDaysByDay.get(dayOfWeek))
            ).longValue());
        }
        Long overallAverage = divide(
                BigDecimal.valueOf(totalByDay.values().stream().reduce(0L, Long::sum)),
                BigDecimal.valueOf(calendarDaysByDay.values().stream().reduce(0, Integer::sum))
        ).longValue();

        List<SpendingReportWeekdayResponseDTO> weekdays = new ArrayList<>();
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            Long averageAmount = averageByDay.get(dayOfWeek);
            weekdays.add(SpendingReportWeekdayResponseDTO.builder()
                    .code(dayOfWeek.name())
                    .name(resolveDayName(dayOfWeek))
                    .totalAmount(totalByDay.get(dayOfWeek))
                    .averageAmount(averageAmount)
                    .calendarDayCount(calendarDaysByDay.get(dayOfWeek))
                    .relativeStrength(overallAverage == 0L
                            ? BigDecimal.ZERO
                            : divide(
                                    BigDecimal.valueOf(averageAmount).multiply(ONE_HUNDRED),
                                    BigDecimal.valueOf(overallAverage)
                            ))
                    .build());
        }

        Long weekdayTotal = totalByDay.entrySet().stream()
                .filter(entry -> entry.getKey().getValue() <= DayOfWeek.FRIDAY.getValue())
                .map(Map.Entry::getValue)
                .reduce(0L, Long::sum);
        Integer weekdayCount = calendarDaysByDay.entrySet().stream()
                .filter(entry -> entry.getKey().getValue() <= DayOfWeek.FRIDAY.getValue())
                .map(Map.Entry::getValue)
                .reduce(0, Integer::sum);
        Long weekendTotal = totalByDay.get(DayOfWeek.SATURDAY) + totalByDay.get(DayOfWeek.SUNDAY);
        Integer weekendCount = calendarDaysByDay.get(DayOfWeek.SATURDAY)
                + calendarDaysByDay.get(DayOfWeek.SUNDAY);
        Long weekdayAverage = divide(
                BigDecimal.valueOf(weekdayTotal),
                BigDecimal.valueOf(weekdayCount)
        ).longValue();
        Long weekendAverage = divide(
                BigDecimal.valueOf(weekendTotal),
                BigDecimal.valueOf(weekendCount)
        ).longValue();
        BigDecimal weekendRatio = weekdayAverage == 0L
                ? null
                : divide(
                        BigDecimal.valueOf(weekendAverage).multiply(ONE_HUNDRED),
                        BigDecimal.valueOf(weekdayAverage)
                );
        DayOfWeek peakDay = averageByDay.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(DayOfWeek.MONDAY);

        return SpendingReportWeekdayAnalysisResponseDTO.builder()
                .weekdays(weekdays)
                .weekdayDailyAverage(weekdayAverage)
                .weekendDailyAverage(weekendAverage)
                .weekendToWeekdayRatio(weekendRatio)
                .peakDayCode(peakDay.name())
                .peakDayName(resolveDayName(peakDay))
                .peakDayAverage(averageByDay.get(peakDay))
                .topWeekendCategoryName(topWeekendCategory == null
                        ? null
                        : resolveCategoryName(topWeekendCategory.getCategory()))
                .build();
    }

    private String resolveSummary(
            AiSpendingReportSummaryModelResponseDTO modelResponse,
            AiSpendingReportSummaryContext context
    ) {
        if (modelResponse == null
                || modelResponse.getSummary() == null
                || modelResponse.getSummary().trim().isEmpty()) {
            return buildFallbackSummary(context);
        }
        return modelResponse.getSummary().trim();
    }

    private String buildFallbackSummary(AiSpendingReportSummaryContext context) {
        if (context.getTransactionCount() == 0) {
            return "가입 이후 확인된 지출 내역이 아직 없어요. 내역이 쌓이면 기간별 흐름과 요일별 소비 패턴을 함께 분석해 드릴게요.";
        }
        String firstSentence = "가입 이후 총 " + formatAmount(context.getTotalExpense())
                + "원을 사용했고, 가장 큰 비중은 "
                + (context.getTopCategoryName() == null ? "확인된 소비" : context.getTopCategoryName())
                + "예요.";
        if (context.getWeekendToWeekdayRatio() == null) {
            return firstSentence + " 지출 내역이 조금 더 쌓이면 평일과 주말 소비 차이도 알려드릴게요.";
        }
        String comparison = context.getWeekendToWeekdayRatio().compareTo(ONE_HUNDRED) >= 0
                ? "주말 일평균 지출이 평일보다 높은 편이에요."
                : "평일 일평균 지출이 주말보다 높은 편이에요.";
        return firstSentence + " " + comparison;
    }

    private String resolveCategoryName(String category) {
        if (category == null) {
            return "기타";
        }
        try {
            return ExpenseCategory.valueOf(category).getCategoryName();
        } catch (IllegalArgumentException exception) {
            return "기타";
        }
    }

    private String resolveDayName(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "월요일";
            case TUESDAY -> "화요일";
            case WEDNESDAY -> "수요일";
            case THURSDAY -> "목요일";
            case FRIDAY -> "금요일";
            case SATURDAY -> "토요일";
            case SUNDAY -> "일요일";
        };
    }

    private String formatAmount(Long amount) {
        return String.format("%,d", amount);
    }

    private BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return dividend.divide(divisor, 1, RoundingMode.HALF_UP);
    }

    private Long orZero(Long value) {
        return value == null ? 0L : value;
    }

    private Integer orZero(Integer value) {
        return value == null ? 0 : value;
    }
}
