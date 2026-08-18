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
import com.project.domain.goal.dto.GoalCreateItemResponseDTO;
import com.project.domain.goal.dto.GoalDecisionRequestDTO;
import com.project.domain.goal.dto.GoalDeleteResponseDTO;
import com.project.domain.goal.dto.GoalItemCreateRequestDTO;
import com.project.domain.goal.dto.GoalItemResponseDTO;
import com.project.domain.goal.dto.GoalResponseDTO;
import com.project.domain.goal.dto.GoalStatusUpdateResponseDTO;
import com.project.domain.goal.dto.GoalUpdateRequestDTO;
import com.project.domain.goal.dto.GoalUpdateResponseDTO;
import com.project.domain.goal.mapper.GoalMapper;
import com.project.domain.notification.dto.NotificationCreateRequestDTO;
import com.project.domain.notification.service.NotificationService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Log4j2
@Service
public class GoalServiceImpl implements GoalService {

    private static final Long RATIO_TOTAL = 100L;
    private static final String TARGET_TYPE_GOAL = "GOAL";
    private static final String GOAL_CREATED = "GOAL_CREATED";
    private static final String GOAL_CHANGED = "GOAL_CHANGED";
    private static final String GOAL_APPROVAL_REQUESTED = "GOAL_APPROVAL_REQUESTED";
    private static final String GOAL_APPROVED = "GOAL_APPROVED";
    private static final String GOAL_REJECTED = "GOAL_REJECTED";
    private static final String LOG_MARKER = "----------";

    private final GoalMapper goalMapper;
    private final CoupleService coupleService;
    private final NotificationService notificationService;
    private final Clock clock = Clock.systemDefaultZone();

    public GoalServiceImpl(GoalMapper goalMapper, CoupleService coupleService) {
        this(goalMapper, coupleService, null);
    }

    @Autowired
    public GoalServiceImpl(
            GoalMapper goalMapper,
            CoupleService coupleService,
            NotificationService notificationService
    ) {
        this.goalMapper = goalMapper;
        this.coupleService = coupleService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public GoalCreateResponseDTO createGoal(
            Long authenticatedUserId,
            GoalCreateRequestDTO request
    ) {
        validateAuthenticatedUser(authenticatedUserId);
        Long totalBudget = validateRequest(request);
        CoupleDTO couple = coupleService.getConnectedCoupleByUserId(authenticatedUserId);
        goalMapper.selectCoupleIdForUpdate(couple.getId());
        boolean isFirstGoal = !Boolean.TRUE.equals(
                goalMapper.existsGoalByCoupleId(couple.getId())
        );

        LocalDateTime createdAt = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        GoalVO goal = buildGoal(
                couple.getId(),
                request,
                totalBudget,
                isFirstGoal,
                createdAt
        );
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

        createGoalNotification(
                findPartnerId(authenticatedUserId, couple),
                GOAL_CREATED,
                "새 시안이 생성되었습니다.",
                goal.getName() + " 시안이 생성되었습니다.",
                goal.getId()
        );

        return GoalCreateResponseDTO.builder()
                .goalId(goal.getId())
                .name(goal.getName())
                .weddingDate(goal.getWeddingDate())
                .region(goal.getRegion())
                .totalBudget(goal.getTotalBudget())
                .groomRatio(goal.getGroomRatio())
                .brideRatio(goal.getBrideRatio())
                .status(goal.getStatus())
                .createdAt(createdAt)
                .items(request.getItems().stream()
                        .map(GoalCreateItemResponseDTO::from)
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalResponseDTO> getGoals(Long authenticatedUserId) {
        validateAuthenticatedUser(authenticatedUserId);
        CoupleDTO couple = coupleService.getConnectedCoupleByUserId(authenticatedUserId);
        List<GoalVO> goals = goalMapper.selectGoalsByCoupleId(couple.getId());
        if (goals == null || goals.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> goalIds = goals.stream()
                .map(GoalVO::getId)
                .collect(Collectors.toList());
        List<GoalItemVO> goalItems = goalMapper.selectGoalItemsByGoalIds(goalIds);
        if (goalItems == null) {
            goalItems = Collections.emptyList();
        }
        Map<Long, List<GoalItemResponseDTO>> itemsByGoalId = goalItems.stream()
                .collect(Collectors.groupingBy(
                        GoalItemVO::getGoalId,
                        Collectors.mapping(GoalItemResponseDTO::from, Collectors.toList())
                ));

        return goals.stream()
                .map(goal -> GoalResponseDTO.from(
                        goal,
                        itemsByGoalId.getOrDefault(goal.getId(), Collections.emptyList()),
                        authenticatedUserId
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public GoalUpdateResponseDTO updateGoal(
            Long authenticatedUserId,
            Long goalId,
            GoalUpdateRequestDTO request
    ) {
        validateAuthenticatedUser(authenticatedUserId);
        validateGoalId(goalId);
        Long totalBudget = validateUpdateRequest(request);
        CoupleDTO couple = coupleService.getConnectedCoupleByUserId(authenticatedUserId);
        GoalVO existingGoal = findGoal(lockCoupleGoals(couple.getId()), goalId);

        GoalVO updatedGoal = GoalVO.builder()
                .id(goalId)
                .coupleId(couple.getId())
                .name(request.getName().trim())
                .weddingDate(request.getWeddingDate())
                .region(request.getRegion().trim())
                .totalBudget(totalBudget)
                .groomRatio(request.getGroomRatio())
                .brideRatio(request.getBrideRatio())
                .build();
        ensureSingleRowUpdated(
                goalMapper.updateGoal(updatedGoal),
                "결혼 목표 수정에 실패했습니다."
        );

        goalMapper.deleteGoalItems(goalId);
        List<GoalItemVO> items = request.getItems().stream()
                .map(this::toGoalItem)
                .collect(Collectors.toList());
        Integer insertedItemCount = goalMapper.insertGoalItems(goalId, items);
        if (insertedItemCount == null || insertedItemCount != items.size()) {
            throw new CustomException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "결혼 목표 항목 수정에 실패했습니다."
            );
        }

        createGoalNotification(
                findPartnerId(authenticatedUserId, couple),
                GOAL_CHANGED,
                "시안이 수정되었습니다.",
                updatedGoal.getName() + " 시안이 수정되었습니다.",
                goalId
        );

        return GoalUpdateResponseDTO.builder()
                .goalId(goalId)
                .name(updatedGoal.getName())
                .weddingDate(updatedGoal.getWeddingDate())
                .region(updatedGoal.getRegion())
                .totalBudget(updatedGoal.getTotalBudget())
                .groomRatio(updatedGoal.getGroomRatio())
                .brideRatio(updatedGoal.getBrideRatio())
                .status(existingGoal.getStatus())
                .isMain(existingGoal.getIsMain())
                .createdAt(existingGoal.getCreatedAt())
                .items(request.getItems().stream()
                        .map(GoalCreateItemResponseDTO::from)
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional
    public GoalDeleteResponseDTO deleteGoal(Long authenticatedUserId, Long goalId) {
        validateAuthenticatedUser(authenticatedUserId);
        validateGoalId(goalId);
        CoupleDTO couple = coupleService.getConnectedCoupleByUserId(authenticatedUserId);
        GoalVO goal = findGoal(lockCoupleGoals(couple.getId()), goalId);
        if (Boolean.TRUE.equals(goal.getIsMain())) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "메인 시안은 삭제할 수 없습니다."
            );
        }

        ensureSingleRowUpdated(
                goalMapper.deleteGoal(goalId, couple.getId()),
                "결혼 목표 삭제에 실패했습니다."
        );
        return GoalDeleteResponseDTO.builder()
                .goalId(goalId)
                .build();
    }

    @Override
    @Transactional
    public GoalStatusUpdateResponseDTO requestMainGoal(
            Long authenticatedUserId,
            Long goalId
    ) {
        validateAuthenticatedUser(authenticatedUserId);
        validateGoalId(goalId);
        CoupleDTO couple = coupleService.getConnectedCoupleByUserId(authenticatedUserId);
        List<GoalVO> goals = lockCoupleGoals(couple.getId());
        GoalVO goal = findGoal(goals, goalId);

        if (!GoalStatus.NORMAL.name().equals(goal.getStatus())
                || Boolean.TRUE.equals(goal.getIsMain())) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "일반 상태의 시안만 메인 시안으로 요청할 수 있습니다."
            );
        }
        boolean hasPendingGoal = goals.stream()
                .anyMatch(candidate -> GoalStatus.PENDING.name().equals(candidate.getStatus()));
        if (hasPendingGoal) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "이미 승인 대기 중인 메인 시안이 있습니다."
            );
        }

        LocalDateTime requestedAt = now();
        ensureSingleRowUpdated(
                goalMapper.updateGoalRequest(goalId, authenticatedUserId, requestedAt),
                "메인 시안 승인 요청에 실패했습니다."
        );
        createGoalNotification(
                findPartnerId(authenticatedUserId, couple),
                GOAL_APPROVAL_REQUESTED,
                "메인 시안 승인 요청이 도착했습니다.",
                goal.getName() + " 시안의 메인 설정 승인을 요청했습니다.",
                goalId
        );

        return buildStatusResponse(
                goalId,
                GoalStatus.PENDING,
                false,
                true,
                requestedAt
        );
    }

    @Override
    @Transactional
    public GoalStatusUpdateResponseDTO cancelMainGoalRequest(
            Long authenticatedUserId,
            Long goalId
    ) {
        validateAuthenticatedUser(authenticatedUserId);
        validateGoalId(goalId);
        CoupleDTO couple = coupleService.getConnectedCoupleByUserId(authenticatedUserId);
        GoalVO goal = findGoal(lockCoupleGoals(couple.getId()), goalId);

        if (!GoalStatus.PENDING.name().equals(goal.getStatus())) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "승인 대기 중인 시안만 요청을 취소할 수 있습니다."
            );
        }
        if (!authenticatedUserId.equals(goal.getRequestedBy())) {
            throw new CustomException(
                    ErrorCode.FORBIDDEN,
                    "메인 시안을 요청한 사용자만 요청을 취소할 수 있습니다."
            );
        }

        ensureSingleRowUpdated(
                goalMapper.cancelGoalRequest(goalId, authenticatedUserId),
                "메인 시안 승인 요청 취소에 실패했습니다."
        );
        return buildStatusResponse(goalId, GoalStatus.NORMAL, false, false, null);
    }

    @Override
    @Transactional
    public GoalStatusUpdateResponseDTO decideMainGoalRequest(
            Long authenticatedUserId,
            Long goalId,
            GoalDecisionRequestDTO request
    ) {
        validateAuthenticatedUser(authenticatedUserId);
        validateGoalId(goalId);
        String decision = validateDecision(request);
        CoupleDTO couple = coupleService.getConnectedCoupleByUserId(authenticatedUserId);
        GoalVO goal = findGoal(lockCoupleGoals(couple.getId()), goalId);

        if (!GoalStatus.PENDING.name().equals(goal.getStatus())
                || goal.getRequestedBy() == null) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "승인 대기 중인 시안만 승인 또는 거절할 수 있습니다."
            );
        }
        if (authenticatedUserId.equals(goal.getRequestedBy())) {
            throw new CustomException(
                    ErrorCode.FORBIDDEN,
                    "메인 시안을 요청한 사용자는 직접 승인하거나 거절할 수 없습니다."
            );
        }

        if ("REJECT".equals(decision)) {
            ensureSingleRowUpdated(
                    goalMapper.rejectGoalRequest(goalId),
                    "메인 시안 승인 요청 거절에 실패했습니다."
            );
            createGoalNotification(
                    goal.getRequestedBy(),
                    GOAL_REJECTED,
                    "메인 시안 요청이 거절되었습니다.",
                    goal.getName() + " 시안의 메인 설정 요청이 거절되었습니다.",
                    goalId
            );
            return buildStatusResponse(goalId, GoalStatus.NORMAL, false, false, null);
        }

        goalMapper.clearMainGoal(couple.getId(), goalId);
        ensureSingleRowUpdated(
                goalMapper.confirmGoal(goalId),
                "메인 시안 승인에 실패했습니다."
        );
        createGoalNotification(
                goal.getRequestedBy(),
                GOAL_APPROVED,
                "메인 시안 요청이 승인되었습니다.",
                goal.getName() + " 시안이 메인 시안으로 승인되었습니다.",
                goalId
        );
        return buildStatusResponse(goalId, GoalStatus.CONFIRMED, true, false, null);
    }

    private void validateAuthenticatedUser(Long authenticatedUserId) {
        if (authenticatedUserId == null || authenticatedUserId <= 0) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void validateGoalId(Long goalId) {
        if (goalId == null || goalId <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "목표 ID가 올바르지 않습니다.");
        }
    }

    private List<GoalVO> lockCoupleGoals(Long coupleId) {
        List<GoalVO> goals = goalMapper.selectGoalsByCoupleIdForUpdate(coupleId);
        return goals == null ? Collections.emptyList() : goals;
    }

    private GoalVO findGoal(List<GoalVO> goals, Long goalId) {
        return goals.stream()
                .filter(goal -> goalId.equals(goal.getId()))
                .findFirst()
                .orElseThrow(() -> new CustomException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "결혼 목표를 찾을 수 없습니다."
                ));
    }

    private String validateDecision(GoalDecisionRequestDTO request) {
        if (request == null || request.getDecision() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "승인 여부는 필수입니다.");
        }
        String decision = request.getDecision().trim().toUpperCase(Locale.ROOT);
        if (!"APPROVE".equals(decision) && !"REJECT".equals(decision)) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "승인 여부는 APPROVE 또는 REJECT여야 합니다."
            );
        }
        return decision;
    }

    private void ensureSingleRowUpdated(Integer updatedCount, String message) {
        if (updatedCount == null || updatedCount != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, message);
        }
    }

    private GoalStatusUpdateResponseDTO buildStatusResponse(
            Long goalId,
            GoalStatus status,
            Boolean isMain,
            Boolean requestedByMe,
            LocalDateTime requestedAt
    ) {
        return GoalStatusUpdateResponseDTO.builder()
                .goalId(goalId)
                .status(status.name())
                .isMain(isMain)
                .requestedByMe(requestedByMe)
                .requestedAt(requestedAt)
                .build();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    private Long validateRequest(GoalCreateRequestDTO request) {
        if (request == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "요청 정보가 필요합니다.");
        }
        validateText(request.getName(), 50, "시안 제목");
        validateText(request.getRegion(), 50, "결혼식 지역");
        validateWeddingDate(request.getWeddingDate());
        validateRatios(request.getGroomRatio(), request.getBrideRatio());
        return calculateTotalBudget(request.getItems());
    }

    private Long validateUpdateRequest(GoalUpdateRequestDTO request) {
        if (request == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "요청 정보가 필요합니다.");
        }
        validateText(request.getName(), 50, "시안 제목");
        validateText(request.getRegion(), 50, "결혼식 지역");
        validateWeddingDate(request.getWeddingDate());
        validateRatios(request.getGroomRatio(), request.getBrideRatio());
        return calculateTotalBudget(request.getItems());
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

    private Long calculateTotalBudget(List<GoalItemCreateRequestDTO> items) {
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
        return includedAmountSum;
    }

    private void validateItem(GoalItemCreateRequestDTO item) {
        if (item == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "목표 항목이 올바르지 않습니다.");
        }
        ExpenseCategory category = ExpenseCategory.fromKorean(trim(item.getCategory()));
        if (!category.isWedding()) {
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

    private GoalVO buildGoal(
            Long coupleId,
            GoalCreateRequestDTO request,
            Long totalBudget,
            Boolean isMain,
            LocalDateTime createdAt
    ) {
        return GoalVO.builder()
                .coupleId(coupleId)
                .name(request.getName().trim())
                .weddingDate(request.getWeddingDate())
                .region(request.getRegion().trim())
                .totalBudget(totalBudget)
                .groomRatio(request.getGroomRatio())
                .brideRatio(request.getBrideRatio())
                .status(Boolean.TRUE.equals(isMain)
                        ? GoalStatus.CONFIRMED.name()
                        : GoalStatus.NORMAL.name())
                .isMain(isMain)
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

    private Long findPartnerId(Long userId, CoupleDTO couple) {
        if (couple == null || userId == null) {
            return null;
        }
        if (userId.equals(couple.getGroomId())) {
            return couple.getBrideId();
        }
        if (userId.equals(couple.getBrideId())) {
            return couple.getGroomId();
        }
        return null;
    }

    private void createGoalNotification(
            Long receiverId,
            String type,
            String title,
            String content,
            Long goalId
    ) {
        if (notificationService == null || receiverId == null) {
            return;
        }
        NotificationCreateRequestDTO request = NotificationCreateRequestDTO.builder()
                .userId(receiverId)
                .type(type)
                .title(title)
                .content(content)
                .targetType(TARGET_TYPE_GOAL)
                .targetId(goalId)
                .build();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            log.info("{} 목표 알림 afterCommit 등록 - userId: {}, type: {}, goalId: {}",
                    LOG_MARKER, request.getUserId(), request.getType(), request.getTargetId());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendGoalNotification(request);
                }
            });
            return;
        }

        sendGoalNotification(request);
    }

    private void sendGoalNotification(NotificationCreateRequestDTO request) {
        try {
            notificationService.createNotification(request);
        } catch (RuntimeException exception) {
            log.info("{} 목표 알림 생성 예외 발생 - userId: {}, type: {}, goalId: {}, "
                            + "exceptionType: {}, message: {}",
                    LOG_MARKER, request.getUserId(), request.getType(), request.getTargetId(),
                    exception.getClass().getName(), exception.getMessage(), exception);
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
