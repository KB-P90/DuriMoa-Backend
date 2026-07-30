package com.project.domain.goal.service;

import com.project.common.enums.ExpenseCategory;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.couple.dto.CoupleDTO;
import com.project.domain.couple.service.CoupleService;
import com.project.domain.goal.domain.GoalBudgetType;
import com.project.domain.goal.domain.GoalItemVO;
import com.project.domain.goal.domain.GoalStatus;
import com.project.domain.goal.domain.GoalVO;
import com.project.domain.goal.dto.GoalCreateRequestDTO;
import com.project.domain.goal.dto.GoalCreateResponseDTO;
import com.project.domain.goal.dto.GoalItemCreateRequestDTO;
import com.project.domain.goal.mapper.GoalMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoalServiceImpl implements GoalService {

    private static final Long RATIO_TOTAL = 100L;
    private static final Set<ExpenseCategory> ALLOWED_CATEGORIES = Set.of(
            ExpenseCategory.WEDDINGHALL,
            ExpenseCategory.STUDIO,
            ExpenseCategory.DRESS,
            ExpenseCategory.MAKEUP,
            ExpenseCategory.EMERGENCY
    );

    private final GoalMapper goalMapper;
    private final CoupleService coupleService;
    private final Clock clock = Clock.systemDefaultZone();

    @Override
    @Transactional
    public GoalCreateResponseDTO createGoal(
            Long authenticatedUserId,
            GoalCreateRequestDTO request
    ) {
        validateAuthenticatedUser(authenticatedUserId);
        validateRequest(request);
        CoupleDTO couple = coupleService.getConnectedCoupleByUserId(authenticatedUserId);

        LocalDateTime createdAt = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        GoalVO goal = buildGoal(couple.getId(), request, createdAt);
        Integer insertedGoalCount = goalMapper.insertGoal(goal);
        if (insertedGoalCount == null || insertedGoalCount != 1 || goal.getId() == null) {
            throw new CustomException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "결혼 목표 저장에 실패했습니다."
            );
        }

        List<GoalItemVO> items = request.getItems().stream()
                .map(this::toGoalItem)
                .collect(Collectors.toList());
        Integer insertedItemCount = goalMapper.insertGoalItems(goal.getId(), items);
        if (insertedItemCount == null || insertedItemCount != items.size()) {
            throw new CustomException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "결혼 목표 항목 저장에 실패했습니다."
            );
        }

        return GoalCreateResponseDTO.builder()
                .goalId(goal.getId())
                .status(GoalStatus.NORMAL.name())
                .createdAt(createdAt)
                .build();
    }

    private void validateAuthenticatedUser(Long authenticatedUserId) {
        if (authenticatedUserId == null || authenticatedUserId <= 0) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void validateRequest(GoalCreateRequestDTO request) {
        if (request == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "요청 정보가 필요합니다.");
        }
        validateText(request.getName(), 50, "시안 제목");
        validateText(request.getRegion(), 50, "결혼식 지역");
        validateWeddingDate(request.getWeddingDate());
        validateRatios(request.getGroomRatio(), request.getBrideRatio());
        validateItems(request.getItems(), request.getTotalBudget());
    }

    private void validateText(String value, Integer maxLength, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    fieldName + "은 필수입니다."
            );
        }
        if (value.trim().length() > maxLength) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    fieldName + "은 " + maxLength + "자 이하여야 합니다."
            );
        }
    }

    private void validateWeddingDate(LocalDate weddingDate) {
        if (weddingDate == null || !weddingDate.isAfter(LocalDate.now(clock))) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "결혼식 날짜는 오늘 이후여야 합니다."
            );
        }
    }

    private void validateRatios(Long groomRatio, Long brideRatio) {
        if (groomRatio == null || brideRatio == null
                || groomRatio < 0 || groomRatio > 100
                || brideRatio < 0 || brideRatio > 100
                || groomRatio + brideRatio != RATIO_TOTAL) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "신랑과 신부의 분담 비율 합계는 100이어야 합니다."
            );
        }
    }

    private void validateItems(List<GoalItemCreateRequestDTO> items, Long totalBudget) {
        if (items == null || items.isEmpty()) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "목표 항목은 최소 1개 이상이어야 합니다."
            );
        }
        Set<String> categories = new HashSet<>();
        Long includedAmountSum = 0L;
        for (GoalItemCreateRequestDTO item : items) {
            validateItem(item);
            String category = item.getCategory().trim();
            if (!categories.add(category)) {
                throw new CustomException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "동일한 목표 항목 카테고리를 중복 등록할 수 없습니다."
                );
            }
            if (Boolean.TRUE.equals(item.getIncluded())) {
                try {
                    includedAmountSum = Math.addExact(includedAmountSum, item.getTargetAmount());
                } catch (ArithmeticException exception) {
                    throw new CustomException(
                            ErrorCode.INVALID_INPUT_VALUE,
                            "목표 금액 합계가 허용 범위를 초과했습니다."
                    );
                }
            }
        }
        if (totalBudget == null || totalBudget < 0 || !totalBudget.equals(includedAmountSum)) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "총예산은 예산에 포함된 목표 항목 금액의 합계와 같아야 합니다."
            );
        }
    }

    private void validateItem(GoalItemCreateRequestDTO item) {
        if (item == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "목표 항목이 올바르지 않습니다.");
        }
        ExpenseCategory category = ExpenseCategory.fromKorean(trim(item.getCategory()));
        if (!ALLOWED_CATEGORIES.contains(category)) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "허용되지 않은 결혼 목표 카테고리입니다."
            );
        }
        if (item.getTargetAmount() == null || item.getTargetAmount() < 0) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "목표 금액은 0 이상이어야 합니다."
            );
        }
        if (item.getIncluded() == null) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "예산 포함 여부는 필수입니다."
            );
        }
        if (!GoalBudgetType.supports(trim(item.getType()))) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "예산 유형은 알뜰형, 균형형, 플렉스형 중 하나여야 합니다."
            );
        }
    }

    private GoalVO buildGoal(Long coupleId, GoalCreateRequestDTO request, LocalDateTime createdAt) {
        return GoalVO.builder()
                .coupleId(coupleId)
                .name(request.getName().trim())
                .weddingDate(request.getWeddingDate())
                .region(request.getRegion().trim())
                .totalBudget(request.getTotalBudget())
                .groomRatio(request.getGroomRatio())
                .brideRatio(request.getBrideRatio())
                .createdAt(createdAt)
                .build();
    }

    private GoalItemVO toGoalItem(GoalItemCreateRequestDTO item) {
        return GoalItemVO.builder()
                .category(item.getCategory().trim())
                .targetAmount(item.getTargetAmount())
                .included(item.getIncluded())
                .type(item.getType().trim())
                .build();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
