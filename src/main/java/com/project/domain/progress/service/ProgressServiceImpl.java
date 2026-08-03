package com.project.domain.progress.service;

import com.project.common.enums.ExpenseCategory;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.couple.dto.CoupleDTO;
import com.project.domain.couple.service.CoupleService;
import com.project.domain.progress.dto.ActiveGoalDTO;
import com.project.domain.progress.dto.CategoryExpenseDTO;
import com.project.domain.progress.dto.GoalItemSourceDTO;
import com.project.domain.progress.dto.MemberSourceDTO;
import com.project.domain.progress.dto.MonthlyExpenseDTO;
import com.project.domain.progress.dto.ProgressResponseDTO;
import com.project.domain.progress.dto.ProgressResponseDTO.GoalItemProgressDTO;
import com.project.domain.progress.dto.ProgressResponseDTO.MemberProgressDTO;
import com.project.domain.progress.dto.ProgressResponseDTO.MonthlyProgressDTO;
import com.project.domain.progress.dto.ProgressResponseDTO.MonthlyProgressItemDTO;
import com.project.domain.progress.dto.ProgressResponseDTO.OverallProgressDTO;
import com.project.domain.progress.dto.ProgressResponseDTO.PersonalProgressDTO;
import com.project.domain.progress.mapper.ProgressMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProgressServiceImpl implements ProgressService {

    private static final int PERCENT_MULTIPLIER = 100;

    private final CoupleService coupleService;
    private final ProgressMapper progressMapper;

    @Override
    @Transactional(readOnly = true)
    public ProgressResponseDTO getProgress(Long userId) {
        CoupleDTO couple = coupleService.getConnectedCoupleByUserId(userId);
        ActiveGoalDTO activeGoal = progressMapper.findActiveGoal(couple.getId());
        if (activeGoal == null) {
            throw new CustomException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "활성 결혼 목표를 찾을 수 없습니다."
            );
        }

        List<Long> memberIds = List.of(couple.getGroomId(), couple.getBrideId());
        List<String> weddingCategoryCodes = ExpenseCategory.getWeddingCodes();
        Long totalExpenseAmount = defaultAmount(
                progressMapper.findTotalWeddingExpense(memberIds, weddingCategoryCodes)
        );
        Integer overallProgressRate = calculateIntegerRate(
                totalExpenseAmount,
                activeGoal.getTargetAmount()
        );

        return ProgressResponseDTO.builder()
                .overall(buildOverall(activeGoal, memberIds, weddingCategoryCodes,
                        totalExpenseAmount, overallProgressRate))
                .personal(buildPersonal(userId, couple, activeGoal))
                .monthlyProgress(buildMonthlyProgress(activeGoal, memberIds,
                        weddingCategoryCodes, overallProgressRate))
                .build();
    }

    private OverallProgressDTO buildOverall(
            ActiveGoalDTO activeGoal,
            List<Long> memberIds,
            List<String> weddingCategoryCodes,
            Long totalExpenseAmount,
            Integer overallProgressRate
    ) {
        List<GoalItemSourceDTO> goalItems = nullToEmpty(
                progressMapper.findIncludedGoalItems(activeGoal.getGoalId())
        );
        Map<String, Long> expenseByCategory = nullToEmpty(
                progressMapper.findCategoryWeddingExpenses(memberIds, weddingCategoryCodes)
        ).stream()
                .filter(expense -> expense.getCategoryCode() != null)
                .collect(Collectors.toMap(
                        CategoryExpenseDTO::getCategoryCode,
                        expense -> defaultAmount(expense.getExpenseAmount()),
                        Long::sum
                ));

        List<GoalItemProgressDTO> items = goalItems.stream()
                .map(item -> buildGoalItem(item, expenseByCategory))
                .collect(Collectors.toList());
        int completedItemCount = (int) items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getCompleted()))
                .count();

        return OverallProgressDTO.builder()
                .targetAmount(activeGoal.getTargetAmount())
                .currentAmount(totalExpenseAmount)
                .progressRate(overallProgressRate)
                .completedItemCount(completedItemCount)
                .totalItemCount(items.size())
                .items(items)
                .build();
    }

    private GoalItemProgressDTO buildGoalItem(
            GoalItemSourceDTO item,
            Map<String, Long> expenseByCategory
    ) {
        ExpenseCategory category = resolveExpenseCategory(item.getCategory());
        Long currentAmount = expenseByCategory.getOrDefault(category.name(), 0L);
        Integer progressRate = currentAmount == 0L
                ? null
                : calculateIntegerRate(currentAmount, item.getTargetAmount());

        return GoalItemProgressDTO.builder()
                .goalItemId(item.getGoalItemId())
                .category(category.getCategoryName())
                .targetAmount(item.getTargetAmount())
                .currentAmount(currentAmount)
                .progressRate(progressRate)
                .completed(Boolean.TRUE.equals(item.getCompleted()))
                .build();
    }

    private PersonalProgressDTO buildPersonal(
            Long userId,
            CoupleDTO couple,
            ActiveGoalDTO activeGoal
    ) {
        List<MemberSourceDTO> members = nullToEmpty(
                progressMapper.findMembers(couple.getGroomId(), couple.getBrideId())
        );
        Map<Long, MemberSourceDTO> memberById = members.stream()
                .filter(member -> member.getUserId() != null)
                .collect(Collectors.toMap(
                        MemberSourceDTO::getUserId,
                        Function.identity(),
                        (left, right) -> left
                ));

        List<MemberProgressDTO> responseMembers = new ArrayList<>();
        addIfNotNull(responseMembers, buildMember(memberById.get(userId), couple, activeGoal));
        addIfNotNull(responseMembers, buildMember(
                memberById.get(findPartnerId(userId, couple)),
                couple,
                activeGoal
        ));

        return PersonalProgressDTO.builder()
                .members(responseMembers)
                .build();
    }

    private MemberProgressDTO buildMember(
            MemberSourceDTO member,
            CoupleDTO couple,
            ActiveGoalDTO activeGoal
    ) {
        if (member == null) {
            return null;
        }

        boolean assetShared = Boolean.TRUE.equals(member.getAssetShared());
        Long targetAmount = calculateMemberTargetAmount(member.getUserId(), couple, activeGoal);
        Long currentAmount = defaultAmount(member.getAsset());
        Double progressRate = calculateDecimalRate(currentAmount, targetAmount);

        return MemberProgressDTO.builder()
                .userId(member.getUserId())
                .name(member.getName())
                .assetShared(assetShared)
                .assetAmount(assetShared ? currentAmount : null)
                .targetAmount(targetAmount)
                .currentAmount(assetShared ? currentAmount : null)
                .progressRate(progressRate)
                .build();
    }

    private MonthlyProgressDTO buildMonthlyProgress(
            ActiveGoalDTO activeGoal,
            List<Long> memberIds,
            List<String> weddingCategoryCodes,
            Integer overallProgressRate
    ) {
        YearMonth startMonth = resolveStartMonth(activeGoal.getCreatedAt());
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startAt = startMonth.atDay(1).atStartOfDay();
        LocalDateTime endAt = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        Map<YearMonth, Long> expenseByMonth = nullToEmpty(
                progressMapper.findMonthlyWeddingExpenses(
                        memberIds,
                        weddingCategoryCodes,
                        startAt,
                        endAt
                )
        ).stream()
                .filter(monthlyExpense -> monthlyExpense.getYearMonth() != null)
                .collect(Collectors.toMap(
                        monthlyExpense -> YearMonth.parse(monthlyExpense.getYearMonth()),
                        monthlyExpense -> defaultAmount(monthlyExpense.getExpenseAmount()),
                        Long::sum
                ));

        List<MonthlyProgressItemDTO> months = new ArrayList<>();
        Long cumulativeExpenseAmount = 0L;
        for (YearMonth month = startMonth;
             !month.isAfter(currentMonth);
             month = month.plusMonths(1)) {
            Long expenseAmount = expenseByMonth.getOrDefault(month, 0L);
            cumulativeExpenseAmount += expenseAmount;
            months.add(MonthlyProgressItemDTO.builder()
                    .yearMonth(month.toString())
                    .expenseAmount(expenseAmount)
                    .cumulativeExpenseAmount(cumulativeExpenseAmount)
                    .progressRate(calculateIntegerRate(
                            cumulativeExpenseAmount,
                            activeGoal.getTargetAmount()
                    ))
                    .build());
        }

        months.sort(Comparator.comparing(MonthlyProgressItemDTO::getYearMonth).reversed());
        return MonthlyProgressDTO.builder()
                .overallProgressRate(overallProgressRate)
                .months(months)
                .build();
    }

    private ExpenseCategory resolveExpenseCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "카테고리 정보가 올바르지 않습니다.");
        }

        String trimmedCategory = category.trim();
        try {
            return ExpenseCategory.valueOf(trimmedCategory);
        } catch (IllegalArgumentException exception) {
            return ExpenseCategory.fromKorean(trimmedCategory);
        }
    }

    private Long calculateMemberTargetAmount(
            Long memberId,
            CoupleDTO couple,
            ActiveGoalDTO activeGoal
    ) {
        Long ratio = memberId.equals(couple.getGroomId())
                ? activeGoal.getGroomRatio()
                : activeGoal.getBrideRatio();
        if (activeGoal.getTargetAmount() == null || ratio == null) {
            return null;
        }
        return BigDecimal.valueOf(activeGoal.getTargetAmount())
                .multiply(BigDecimal.valueOf(ratio))
                .divide(BigDecimal.valueOf(PERCENT_MULTIPLIER), 0, RoundingMode.HALF_UP)
                .longValue();
    }

    private Integer calculateIntegerRate(Long currentAmount, Long targetAmount) {
        if (targetAmount == null || targetAmount == 0) {
            return null;
        }
        return BigDecimal.valueOf(defaultAmount(currentAmount))
                .multiply(BigDecimal.valueOf(PERCENT_MULTIPLIER))
                .divide(BigDecimal.valueOf(targetAmount), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private Double calculateDecimalRate(Long currentAmount, Long targetAmount) {
        if (targetAmount == null || targetAmount == 0) {
            return null;
        }
        return BigDecimal.valueOf(defaultAmount(currentAmount))
                .multiply(BigDecimal.valueOf(PERCENT_MULTIPLIER))
                .divide(BigDecimal.valueOf(targetAmount), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private YearMonth resolveStartMonth(LocalDateTime createdAt) {
        if (createdAt == null) {
            return YearMonth.from(LocalDate.now());
        }
        return YearMonth.from(createdAt);
    }

    private Long findPartnerId(Long userId, CoupleDTO couple) {
        if (userId.equals(couple.getGroomId())) {
            return couple.getBrideId();
        }
        return couple.getGroomId();
    }

    private Long defaultAmount(Long amount) {
        return amount == null ? 0L : amount;
    }

    private void addIfNotNull(List<MemberProgressDTO> members, MemberProgressDTO member) {
        if (member != null) {
            members.add(member);
        }
    }

    private <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
