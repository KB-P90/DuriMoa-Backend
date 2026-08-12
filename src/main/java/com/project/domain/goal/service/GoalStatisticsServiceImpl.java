package com.project.domain.goal.service;

import com.project.common.enums.ExpenseCategory;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.goal.domain.GoalStatisticsVO;
import com.project.domain.goal.dto.GoalAllRegionsEstimateResponseDTO;
import com.project.domain.goal.dto.GoalBudgetEstimateResponseDTO;
import com.project.domain.goal.dto.GoalBudgetTypeEstimateResponseDTO;
import com.project.domain.goal.dto.GoalRegionEstimateResponseDTO;
import com.project.domain.goal.dto.GoalStatisticsResponseDTO;
import com.project.domain.goal.mapper.GoalStatisticsMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoalStatisticsServiceImpl implements GoalStatisticsService {

    private static final Long ZERO_AMOUNT = 0L;
    private static final List<ExpenseCategory> STATISTICS_CATEGORIES = Arrays.asList(
            ExpenseCategory.WEDDINGHALL,
            ExpenseCategory.STUDIO,
            ExpenseCategory.DRESS,
            ExpenseCategory.MAKEUP
    );
    private static final List<String> ESTIMATE_CATEGORIES =
            STATISTICS_CATEGORIES.stream()
                    .map(ExpenseCategory::getCategoryName)
                    .collect(Collectors.toList());
    private static final List<String> ESTIMATE_REGIONS = Arrays.asList(
            "서울(강남외)",
            "부산",
            "대구",
            "인천",
            "광주",
            "대전",
            "경기도"
    );

    private final GoalStatisticsMapper goalStatisticsMapper;

    @Override
    @Transactional(readOnly = true)
    public GoalStatisticsResponseDTO findGoalStatistics(String region, String category) {
        String normalizedRegion = normalizeRequired(region, "지역을 입력해 주세요.");
        String normalizedCategory = normalizeRequired(category, "카테고리를 입력해 주세요.");

        if (!ESTIMATE_CATEGORIES.contains(normalizedCategory)) {
            return buildEmptyStatistics(normalizedRegion, normalizedCategory);
        }

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

    private GoalStatisticsResponseDTO buildEmptyStatistics(String region, String category) {
        return GoalStatisticsResponseDTO.builder()
                .region(region)
                .category(category)
                .upper10(ZERO_AMOUNT)
                .upper25(ZERO_AMOUNT)
                .median(ZERO_AMOUNT)
                .lower25(ZERO_AMOUNT)
                .lower10(ZERO_AMOUNT)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GoalBudgetEstimateResponseDTO findGoalBudgetEstimate(String region) {
        String normalizedRegion = normalizeRequired(region, "지역을 입력해 주세요.");
        List<GoalStatisticsVO> statistics =
                goalStatisticsMapper.selectGoalStatisticsForEstimate(
                        normalizedRegion,
                        ESTIMATE_CATEGORIES
                );
        validateEstimateStatistics(statistics);

        Long economyTotal = sumAmounts(statistics, GoalStatisticsVO::getLow25);
        Long balancedTotal = sumAmounts(statistics, GoalStatisticsVO::getMid50);
        Long flexTotal = sumAmounts(statistics, GoalStatisticsVO::getHigh25);

        return GoalBudgetEstimateResponseDTO.builder()
                .region(normalizedRegion)
                .estimates(Arrays.asList(
                        buildEstimate("알뜰형", economyTotal),
                        buildEstimate("균형형", balancedTotal),
                        buildEstimate("플렉스형", flexTotal)
                ))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GoalAllRegionsEstimateResponseDTO findAllRegionsGoalBudgetEstimate() {
        List<GoalStatisticsVO> statistics =
                goalStatisticsMapper.selectGoalStatisticsForAllRegionsEstimate(
                        ESTIMATE_REGIONS,
                        ESTIMATE_CATEGORIES
                );
        if (statistics == null) {
            throwIncompleteStatisticsException();
        }

        Map<String, List<GoalStatisticsVO>> statisticsByRegion = statistics.stream()
                .filter(statistic -> statistic != null && statistic.getRegion() != null)
                .collect(Collectors.groupingBy(GoalStatisticsVO::getRegion));

        List<GoalRegionEstimateResponseDTO> regions = ESTIMATE_REGIONS.stream()
                .map(region -> buildRegionEstimate(region, statisticsByRegion.get(region)))
                .collect(Collectors.toList());

        return GoalAllRegionsEstimateResponseDTO.builder()
                .regions(regions)
                .build();
    }

    private GoalRegionEstimateResponseDTO buildRegionEstimate(
            String region,
            List<GoalStatisticsVO> statistics
    ) {
        validateEstimateStatistics(statistics);
        return GoalRegionEstimateResponseDTO.builder()
                .region("서울(강남외)".equals(region) ? "서울" : region)
                .totalAmount(sumAmounts(statistics, GoalStatisticsVO::getMid50))
                .build();
    }

    private void validateEstimateStatistics(List<GoalStatisticsVO> statistics) {
        if (statistics == null) {
            throwIncompleteStatisticsException();
        }

        Map<String, GoalStatisticsVO> statisticsByCategory = statistics.stream()
                .filter(statistic -> statistic != null && statistic.getCategory() != null)
                .collect(Collectors.toMap(
                        GoalStatisticsVO::getCategory,
                        Function.identity(),
                        (first, duplicate) -> first
                ));

        boolean incomplete = ESTIMATE_CATEGORIES.stream()
                .anyMatch(category -> {
                    GoalStatisticsVO statistic = statisticsByCategory.get(category);
                    return statistic == null
                            || statistic.getLow25() == null
                            || statistic.getMid50() == null
                            || statistic.getHigh25() == null;
                });
        if (incomplete) {
            throwIncompleteStatisticsException();
        }
    }

    private Long sumAmounts(
            List<GoalStatisticsVO> statistics,
            Function<GoalStatisticsVO, Long> amountExtractor
    ) {
        return statistics.stream()
                .map(amountExtractor)
                .reduce(0L, Math::addExact);
    }

    private GoalBudgetTypeEstimateResponseDTO buildEstimate(String type, Long totalAmount) {
        return GoalBudgetTypeEstimateResponseDTO.builder()
                .type(type)
                .totalAmount(totalAmount)
                .build();
    }

    private void throwIncompleteStatisticsException() {
        throw new CustomException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "예상 예산 산정에 필요한 통계 정보가 부족합니다."
        );
    }

    private String normalizeRequired(String value, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, errorMessage);
        }
        return value.trim();
    }
}
