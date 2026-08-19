package com.project.domain.progress.service;

import com.project.common.enums.ExpenseCategory;
import com.project.common.enums.SavingCategory;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.couple.dto.CoupleDTO;
import com.project.domain.couple.service.CoupleService;
import com.project.domain.progress.dto.ActiveGoalDTO;
import com.project.domain.progress.dto.CategoryExpenseDTO;
import com.project.domain.progress.dto.GoalItemSourceDTO;
import com.project.domain.progress.dto.MemberSourceDTO;
import com.project.domain.progress.dto.MonthlyProgressResponseDTO;
import com.project.domain.progress.dto.MonthlyProgressResponseDTO.MonthlyProgressItemDTO;
import com.project.domain.progress.dto.MonthlyExpenseDTO;
import com.project.domain.progress.dto.ProgressItemCompletionRequestDTO;
import com.project.domain.progress.dto.ProgressItemCompletionResponseDTO;
import com.project.domain.progress.dto.ProgressResponseDTO;
import com.project.domain.progress.dto.ProgressResponseDTO.GoalItemProgressDTO;
import com.project.domain.progress.dto.ProgressResponseDTO.MemberProgressDTO;
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
        List<String> weddingExpenseCategoryCodes = ExpenseCategory.getWeddingCodes();
        List<MemberSourceDTO> members = nullToEmpty(progressMapper.findMembers(
                couple.getGroomId(), couple.getBrideId(), SavingCategory.getWeddingCodes()
        ));
        Long totalSavedAmount = sumAmounts(members, MemberSourceDTO::getSavingAmount);
        Integer overallProgressRate = calculateIntegerRate(
                totalSavedAmount,
                activeGoal.getTargetAmount()
        );

        return ProgressResponseDTO.builder()
                .overall(buildOverall(activeGoal, memberIds, weddingExpenseCategoryCodes,
                        totalSavedAmount, overallProgressRate))
                .personal(buildPersonal(userId, couple, activeGoal, members))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyProgressResponseDTO getMonthlyProgress(Long userId) {
        CoupleDTO couple = coupleService.getConnectedCoupleByUserId(userId);
        ActiveGoalDTO activeGoal = getActiveGoal(couple.getId());
        List<Long> memberIds = List.of(couple.getGroomId(), couple.getBrideId());
        List<String> weddingSavingCategoryCodes = SavingCategory.getWeddingCodes();
        List<MemberSourceDTO> members = nullToEmpty(progressMapper.findMembers(
                couple.getGroomId(), couple.getBrideId(), weddingSavingCategoryCodes
        ));
        Long weddingFundBaseline = sumAmounts(members, MemberSourceDTO::getWeddingFund);
        Long totalSavedAmount = sumAmounts(members, MemberSourceDTO::getSavingAmount);

        return buildMonthlyProgress(
                activeGoal,
                memberIds,
                weddingSavingCategoryCodes,
                weddingFundBaseline,
                calculateIntegerRate(totalSavedAmount, activeGoal.getTargetAmount())
        );
    }

    @Override
    @Transactional
    public ProgressItemCompletionResponseDTO updateItemCompletion(
            Long userId,
            Long goalItemId,
            ProgressItemCompletionRequestDTO request
    ) {
        CoupleDTO couple = coupleService.getConnectedCoupleByUserId(userId);
        Long goalId = progressMapper.findActiveGoalIdByGoalItemId(goalItemId, couple.getId());
        if (goalId == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "활성 결혼 목표 항목을 찾을 수 없습니다.");
        }

        progressMapper.updateGoalItemCompletion(goalItemId, goalId, request.getCompleted());
        return ProgressItemCompletionResponseDTO.builder()
                .goalItemId(goalItemId)
                .completed(request.getCompleted())
                .completedItemCount(defaultCount(
                        progressMapper.countCompletedIncludedGoalItems(goalId)))
                .totalItemCount(defaultCount(progressMapper.countIncludedGoalItems(goalId)))
                .build();
    }

    private ActiveGoalDTO getActiveGoal(Long coupleId) {
        ActiveGoalDTO activeGoal = progressMapper.findActiveGoal(coupleId);
        if (activeGoal == null) {
            throw new CustomException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "활성 결혼 목표를 찾을 수 없습니다."
            );
        }
        return activeGoal;
    }

    private OverallProgressDTO buildOverall(
            ActiveGoalDTO activeGoal,
            List<Long> memberIds,
            List<String> weddingExpenseCategoryCodes,
            Long totalSavedAmount,
            Integer overallProgressRate
    ) {
        List<GoalItemSourceDTO> goalItems = nullToEmpty(
                progressMapper.findIncludedGoalItems(activeGoal.getGoalId())
        );
        // 카테고리별 항목 진행률은 기존과 동일하게 지출(EXPENSE) 기준을 유지한다.
        Map<String, Long> expenseByCategory = nullToEmpty(
                progressMapper.findCategoryWeddingExpenses(memberIds, weddingExpenseCategoryCodes)
        ).stream()
                .filter(expense -> expense.getCategoryCode() != null)
                .collect(Collectors.toMap(
                        CategoryExpenseDTO::getCategoryCode,
                        expense -> defaultAmount(expense.getExpenseAmount()),
                        Long::sum
        ));

        List<GoalItemProgressDTO> items = goalItems.stream()
                // service_record는 ExpenseCategory 코드 기준이고, goal_item은 표시명으로 저장될 수 있다.
                .map(item -> buildGoalItem(item, expenseByCategory))
                .collect(Collectors.toList());
        int completedItemCount = (int) items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getCompleted()))
                .count();

        return OverallProgressDTO.builder()
                .targetAmount(activeGoal.getTargetAmount())
                .currentAmount(totalSavedAmount)
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
        // goal_item.category를 service_record 카테고리 코드와 매칭하기 전에 정규화한다.
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
            ActiveGoalDTO activeGoal,
            List<MemberSourceDTO> members
    ) {
        Map<Long, MemberSourceDTO> memberById = members.stream()
                .filter(member -> member.getUserId() != null)
                .collect(Collectors.toMap(
                        MemberSourceDTO::getUserId,
                        Function.identity(),
                        (left, right) -> left
                ));

        List<MemberProgressDTO> responseMembers = new ArrayList<>();
        addIfNotNull(responseMembers, buildMember(memberById.get(userId), userId, couple, activeGoal));
        addIfNotNull(responseMembers, buildMember(
                memberById.get(findPartnerId(userId, couple)),
                userId,
                couple,
                activeGoal
        ));

        return PersonalProgressDTO.builder()
                .members(responseMembers)
                .build();
    }

    private MemberProgressDTO buildMember(
            MemberSourceDTO member,
            Long authenticatedUserId,
            CoupleDTO couple,
            ActiveGoalDTO activeGoal
    ) {
        if (member == null) {
            return null;
        }

        boolean assetShared = Boolean.TRUE.equals(member.getAssetShared());
        boolean isOwner = member.getUserId().equals(authenticatedUserId);
        boolean canViewAssetAmount = assetShared || isOwner;
        Long targetAmount = calculateMemberTargetAmount(member.getUserId(), couple, activeGoal);
        // assetAmount는 계좌 실제 잔액 종합, currentAmount는 달성률 계산에 쓰이는 "모은 돈"으로 서로 다른 값이다.
        Long assetAmount = defaultAmount(member.getAssetAmount());
        Long savingAmount = defaultAmount(member.getSavingAmount());
        Double progressRate = calculateDecimalRate(savingAmount, targetAmount);

        // 비공개 자산은 상대방에게만 금액을 숨기고, 본인에게는 정상 제공한다.
        return MemberProgressDTO.builder()
                .userId(member.getUserId())
                .name(member.getName())
                .role(member.getRole())
                .profileImage(member.getProfileImage())
                .assetShared(assetShared)
                .assetAmount(canViewAssetAmount ? assetAmount : null)
                .targetAmount(targetAmount)
                .currentAmount(canViewAssetAmount ? savingAmount : null)
                .progressRate(progressRate)
                .build();
    }

    private MonthlyProgressResponseDTO buildMonthlyProgress(
            ActiveGoalDTO activeGoal,
            List<Long> memberIds,
            List<String> weddingSavingCategoryCodes,
            Long weddingFundBaseline,
            Integer overallProgressRate
    ) {
        LocalDate firstSavingDate = progressMapper.findFirstWeddingSavingDate(
                memberIds,
                weddingSavingCategoryCodes
        );
        YearMonth startMonth = resolveStartMonth(firstSavingDate, activeGoal.getCreatedAt());
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startAt = startMonth.atDay(1).atStartOfDay();
        LocalDateTime endAt = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        // MonthlyExpenseDTO의 expenseAmount 컬럼을 월별로 모은 저축 금액 용도로 재사용한다.
        Map<YearMonth, Long> savingByMonth = nullToEmpty(
                progressMapper.findMonthlyWeddingSaving(
                        memberIds,
                        weddingSavingCategoryCodes,
                        startAt,
                        endAt
                )
        ).stream()
                .filter(monthlySaving -> monthlySaving.getYearMonth() != null)
                .collect(Collectors.toMap(
                        monthlySaving -> YearMonth.parse(monthlySaving.getYearMonth()),
                        monthlySaving -> defaultAmount(monthlySaving.getExpenseAmount()),
                        Long::sum
                ));

        List<MonthlyProgressItemDTO> months = new ArrayList<>();
        // 온보딩에서 입력한 기존 모은 돈(wedding_fund)을 누적액의 시작 기준값으로 더해준다.
        Long cumulativeSavedAmount = defaultAmount(weddingFundBaseline);
        // DB는 저축이 있는 월만 반환하므로 목표 생성월부터 현재월까지 직접 채운다.
        // 월별 달성률은 누적 기준이므로 저축이 없는 월도 이전 누적률을 유지한다.
        for (YearMonth month = startMonth;
             !month.isAfter(currentMonth);
             month = month.plusMonths(1)) {
            Long savedAmount = savingByMonth.getOrDefault(month, 0L);
            cumulativeSavedAmount += savedAmount;
            months.add(MonthlyProgressItemDTO.builder()
                    .yearMonth(month.toString())
                    .expenseAmount(savedAmount)
                    .cumulativeExpenseAmount(cumulativeSavedAmount)
                    .progressRate(calculateIntegerRate(
                            cumulativeSavedAmount,
                            activeGoal.getTargetAmount()
                    ))
                    .build());
        }

        months.sort(Comparator.comparing(MonthlyProgressItemDTO::getYearMonth).reversed());
        return MonthlyProgressResponseDTO.builder()
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

    private YearMonth resolveStartMonth(
            LocalDate firstSavingDate,
            LocalDateTime goalCreatedAt
    ) {
        if (firstSavingDate == null && goalCreatedAt == null) {
            return YearMonth.now();
        }
        if (firstSavingDate == null) {
            return YearMonth.from(goalCreatedAt);
        }
        if (goalCreatedAt == null) {
            return YearMonth.from(firstSavingDate);
        }
        return YearMonth.from(firstSavingDate).isBefore(YearMonth.from(goalCreatedAt))
                ? YearMonth.from(firstSavingDate)
                : YearMonth.from(goalCreatedAt);
    }

    private Long findPartnerId(Long userId, CoupleDTO couple) {
        if (userId.equals(couple.getGroomId())) {
            return couple.getBrideId();
        }
        return couple.getGroomId();
    }

    private Long sumAmounts(List<MemberSourceDTO> members, Function<MemberSourceDTO, Long> amountGetter) {
        return members.stream()
                .map(amountGetter)
                .map(this::defaultAmount)
                .reduce(0L, Long::sum);
    }

    private Long defaultAmount(Long amount) {
        return amount == null ? 0L : amount;
    }

    private Integer defaultCount(Integer count) {
        return count == null ? 0 : count;
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
