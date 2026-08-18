package com.project.domain.goal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.couple.dto.CoupleDTO;
import com.project.domain.couple.service.CoupleService;
import com.project.domain.goal.domain.GoalItemVO;
import com.project.domain.goal.domain.GoalVO;
import com.project.domain.goal.dto.GoalCreateRequestDTO;
import com.project.domain.goal.dto.GoalCreateResponseDTO;
import com.project.domain.goal.dto.GoalDecisionRequestDTO;
import com.project.domain.goal.dto.GoalItemCreateRequestDTO;
import com.project.domain.goal.dto.GoalResponseDTO;
import com.project.domain.goal.dto.GoalStatusUpdateResponseDTO;
import com.project.domain.goal.dto.GoalUpdateRequestDTO;
import com.project.domain.goal.dto.GoalUpdateResponseDTO;
import com.project.domain.goal.dto.GoalDeleteResponseDTO;
import com.project.domain.goal.mapper.GoalMapper;
import com.project.domain.notification.dto.NotificationCreateRequestDTO;
import com.project.domain.notification.service.NotificationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GoalServiceImplTest {

    @Test
    void createGoalStoresGoalAndItemsForConnectedCouple() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        GoalCreateRequestDTO request = validRequest();

        when(coupleService.getConnectedCoupleByUserId(1L))
                .thenReturn(new CoupleDTO(10L, 1L, 2L, "CONNECTED"));
        when(goalMapper.selectCoupleIdForUpdate(10L)).thenReturn(10L);
        when(goalMapper.existsGoalByCoupleId(10L)).thenReturn(false);
        when(goalMapper.insertGoal(any(GoalVO.class))).thenAnswer(invocation -> {
            GoalVO goal = invocation.getArgument(0);
            goal.setId(100L);
            return 1;
        });
        when(goalMapper.insertGoalItems(eq(100L), anyList())).thenReturn(2);

        GoalCreateResponseDTO response = service.createGoal(1L, request);

        assertEquals(100L, response.getGoalId());
        assertEquals("CONFIRMED", response.getStatus());
        assertEquals(35_000_000L, response.getTotalBudget());
        assertEquals(2, response.getItems().size());
        assertEquals(false, response.getItems().get(0).getCompleted());
        ArgumentCaptor<GoalVO> goalCaptor = ArgumentCaptor.forClass(GoalVO.class);
        verify(goalMapper).insertGoal(goalCaptor.capture());
        assertEquals(10L, goalCaptor.getValue().getCoupleId());
        assertEquals(35_000_000L, goalCaptor.getValue().getTotalBudget());
        assertEquals("CONFIRMED", goalCaptor.getValue().getStatus());
        assertEquals(true, goalCaptor.getValue().getIsMain());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GoalItemVO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(goalMapper).insertGoalItems(eq(100L), itemsCaptor.capture());
        assertEquals(2, itemsCaptor.getValue().size());
        assertEquals("예식장", itemsCaptor.getValue().get(0).getCategory());
    }

    @Test
    void createGoalStoresAdditionalGoalAsNonMainForConnectedCouple() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);

        when(coupleService.getConnectedCoupleByUserId(2L))
                .thenReturn(new CoupleDTO(10L, 1L, 2L, "CONNECTED"));
        when(goalMapper.selectCoupleIdForUpdate(10L)).thenReturn(10L);
        when(goalMapper.existsGoalByCoupleId(10L)).thenReturn(true);
        when(goalMapper.insertGoal(any(GoalVO.class))).thenAnswer(invocation -> {
            GoalVO goal = invocation.getArgument(0);
            goal.setId(101L);
            return 1;
        });
        when(goalMapper.insertGoalItems(eq(101L), anyList())).thenReturn(2);

        service.createGoal(2L, validRequest());

        ArgumentCaptor<GoalVO> goalCaptor = ArgumentCaptor.forClass(GoalVO.class);
        verify(goalMapper).selectCoupleIdForUpdate(10L);
        verify(goalMapper).insertGoal(goalCaptor.capture());
        assertEquals("NORMAL", goalCaptor.getValue().getStatus());
        assertEquals(false, goalCaptor.getValue().getIsMain());
    }

    @Test
    void createGoalDoesNotFailWhenNotificationCreationFails() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        NotificationService notificationService = mock(NotificationService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService, notificationService);

        when(coupleService.getConnectedCoupleByUserId(1L))
                .thenReturn(new CoupleDTO(10L, 1L, 2L, "CONNECTED"));
        when(goalMapper.selectCoupleIdForUpdate(10L)).thenReturn(10L);
        when(goalMapper.existsGoalByCoupleId(10L)).thenReturn(true);
        when(goalMapper.insertGoal(any(GoalVO.class))).thenAnswer(invocation -> {
            GoalVO goal = invocation.getArgument(0);
            goal.setId(102L);
            return 1;
        });
        when(goalMapper.insertGoalItems(eq(102L), anyList())).thenReturn(2);
        when(notificationService.createNotification(any(NotificationCreateRequestDTO.class)))
                .thenThrow(new RuntimeException("push device table missing"));

        GoalCreateResponseDTO response = service.createGoal(1L, validRequest());

        assertEquals(102L, response.getGoalId());
        verify(notificationService).createNotification(any(NotificationCreateRequestDTO.class));
    }

    @Test
    void createGoalRejectsUnauthenticatedUser() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.createGoal(null, validRequest())
        );

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verify(coupleService, never()).getConnectedCoupleByUserId(any());
    }

    @Test
    void createGoalRejectsRatioTotalOtherThanOneHundred() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        GoalCreateRequestDTO request = GoalCreateRequestDTO.builder()
                .name("결혼 예산")
                .weddingDate(LocalDate.of(2099, 10, 9))
                .region("서울(강남)")
                .groomRatio(60L)
                .brideRatio(30L)
                .items(validItems())
                .build();

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.createGoal(1L, request)
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        verify(goalMapper, never()).insertGoal(any());
    }

    @Test
    void createGoalRejectsDuplicateCategories() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        GoalItemCreateRequestDTO duplicate = item("예식장", 17_000_000L, true);
        GoalCreateRequestDTO request = GoalCreateRequestDTO.builder()
                .name("결혼 예산")
                .weddingDate(LocalDate.of(2099, 10, 9))
                .region("서울(강남)")
                .groomRatio(50L)
                .brideRatio(50L)
                .items(List.of(item("예식장", 18_000_000L, true), duplicate))
                .build();

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.createGoal(1L, request)
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        verify(goalMapper, never()).insertGoal(any());
    }

    @Test
    void createGoalExcludesNonIncludedItemFromCalculatedBudget() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        GoalCreateRequestDTO request = GoalCreateRequestDTO.builder()
                .name("결혼 예산")
                .weddingDate(LocalDate.of(2099, 10, 9))
                .region("서울(강남)")
                .groomRatio(50L)
                .brideRatio(50L)
                .items(List.of(
                        item("예식장", 18_000_000L, true),
                        item("스튜디오", 40_000_000L, false)
                ))
                .build();
        when(coupleService.getConnectedCoupleByUserId(1L))
                .thenReturn(new CoupleDTO(10L, 1L, 2L, "CONNECTED"));
        when(goalMapper.insertGoal(any(GoalVO.class))).thenAnswer(invocation -> {
            GoalVO goal = invocation.getArgument(0);
            goal.setId(100L);
            return 1;
        });
        when(goalMapper.insertGoalItems(eq(100L), anyList())).thenReturn(2);

        GoalCreateResponseDTO response = service.createGoal(1L, request);

        assertEquals(18_000_000L, response.getTotalBudget());
        ArgumentCaptor<GoalVO> goalCaptor = ArgumentCaptor.forClass(GoalVO.class);
        verify(goalMapper).insertGoal(goalCaptor.capture());
        assertEquals(18_000_000L, goalCaptor.getValue().getTotalBudget());
    }

    @Test
    void createGoalAllowsJewelryWeddingCategory() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        GoalCreateRequestDTO request = GoalCreateRequestDTO.builder()
                .name("예물 포함 결혼 예산")
                .weddingDate(LocalDate.of(2099, 10, 9))
                .region("서울(강남)")
                .groomRatio(50L)
                .brideRatio(50L)
                .items(List.of(item("예물", 5_000_000L, true)))
                .build();
        when(coupleService.getConnectedCoupleByUserId(1L))
                .thenReturn(new CoupleDTO(10L, 1L, 2L, "CONNECTED"));
        when(goalMapper.insertGoal(any(GoalVO.class))).thenAnswer(invocation -> {
            GoalVO goal = invocation.getArgument(0);
            goal.setId(100L);
            return 1;
        });
        when(goalMapper.insertGoalItems(eq(100L), anyList())).thenReturn(1);

        GoalCreateResponseDTO response = service.createGoal(1L, request);

        assertEquals(5_000_000L, response.getTotalBudget());
        assertEquals("예물", response.getItems().get(0).getCategory());
    }

    @Test
    void createGoalRejectsPastWeddingDate() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        GoalCreateRequestDTO request = GoalCreateRequestDTO.builder()
                .name("결혼 예산")
                .weddingDate(LocalDate.of(2020, 1, 1))
                .region("서울(강남)")
                .groomRatio(50L)
                .brideRatio(50L)
                .items(validItems())
                .build();

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.createGoal(1L, request)
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        verify(goalMapper, never()).insertGoal(any());
    }

    @Test
    void getGoalsReturnsGoalsWithGroupedItems() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        when(coupleService.getConnectedCoupleByUserId(1L))
                .thenReturn(new CoupleDTO(10L, 1L, 2L, "CONNECTED"));
        when(goalMapper.selectGoalsByCoupleId(10L)).thenReturn(List.of(
                goal(2L, "확정 예산", "CONFIRMED", true),
                goal(1L, "일반 예산", "NORMAL", false)
        ));
        when(goalMapper.selectGoalItemsByGoalIds(List.of(2L, 1L))).thenReturn(List.of(
                goalItem(4L, 2L, "예식장"),
                goalItem(1L, 1L, "스튜디오"),
                goalItem(2L, 1L, "예비비")
        ));

        List<GoalResponseDTO> response = service.getGoals(1L);

        assertEquals(2, response.size());
        assertEquals(2L, response.get(0).getGoalId());
        assertEquals("CONFIRMED", response.get(0).getStatus());
        assertEquals(1, response.get(0).getItems().size());
        assertEquals(1L, response.get(1).getGoalId());
        assertEquals(2, response.get(1).getItems().size());
        assertEquals("스튜디오", response.get(1).getItems().get(0).getCategory());
        verify(goalMapper).selectGoalsByCoupleId(10L);
        verify(goalMapper).selectGoalItemsByGoalIds(List.of(2L, 1L));
    }

    @Test
    void getGoalsReturnsEmptyListWithoutItemQuery() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        when(coupleService.getConnectedCoupleByUserId(1L))
                .thenReturn(new CoupleDTO(10L, 1L, 2L, "CONNECTED"));
        when(goalMapper.selectGoalsByCoupleId(10L)).thenReturn(Collections.emptyList());

        List<GoalResponseDTO> response = service.getGoals(1L);

        assertEquals(Collections.emptyList(), response);
        verify(goalMapper, never()).selectGoalItemsByGoalIds(anyList());
    }

    @Test
    void requestMainGoalChangesNormalGoalToPending() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        when(coupleService.getConnectedCoupleByUserId(1L))
                .thenReturn(new CoupleDTO(10L, 1L, 2L, "CONNECTED"));
        when(goalMapper.selectGoalsByCoupleIdForUpdate(10L))
                .thenReturn(List.of(goal(1L, "일반 예산", "NORMAL", false)));
        when(goalMapper.updateGoalRequest(eq(1L), eq(1L), any(LocalDateTime.class)))
                .thenReturn(1);

        GoalStatusUpdateResponseDTO response = service.requestMainGoal(1L, 1L);

        assertEquals("PENDING", response.getStatus());
        assertEquals(false, response.getIsMain());
        assertEquals(true, response.getRequestedByMe());
        verify(goalMapper).updateGoalRequest(eq(1L), eq(1L), any(LocalDateTime.class));
    }

    @Test
    void requestMainGoalRejectsWhenAnotherGoalIsPending() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        when(coupleService.getConnectedCoupleByUserId(1L))
                .thenReturn(new CoupleDTO(10L, 1L, 2L, "CONNECTED"));
        GoalVO pendingGoal = goal(2L, "대기 예산", "PENDING", false);
        pendingGoal.setRequestedBy(2L);
        when(goalMapper.selectGoalsByCoupleIdForUpdate(10L)).thenReturn(List.of(
                goal(1L, "일반 예산", "NORMAL", false),
                pendingGoal
        ));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.requestMainGoal(1L, 1L)
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        verify(goalMapper, never()).updateGoalRequest(any(), any(), any());
    }

    @Test
    void cancelMainGoalRequestAllowsRequester() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        GoalVO pendingGoal = goal(1L, "대기 예산", "PENDING", false);
        pendingGoal.setRequestedBy(1L);
        when(coupleService.getConnectedCoupleByUserId(1L))
                .thenReturn(new CoupleDTO(10L, 1L, 2L, "CONNECTED"));
        when(goalMapper.selectGoalsByCoupleIdForUpdate(10L)).thenReturn(List.of(pendingGoal));
        when(goalMapper.cancelGoalRequest(1L, 1L)).thenReturn(1);

        GoalStatusUpdateResponseDTO response = service.cancelMainGoalRequest(1L, 1L);

        assertEquals("NORMAL", response.getStatus());
        assertEquals(false, response.getRequestedByMe());
        verify(goalMapper).cancelGoalRequest(1L, 1L);
    }

    @Test
    void decideMainGoalRequestApprovesPartnerRequestAndReplacesMainGoal() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        GoalVO pendingGoal = goal(2L, "신규 메인", "PENDING", false);
        pendingGoal.setRequestedBy(1L);
        when(coupleService.getConnectedCoupleByUserId(2L))
                .thenReturn(new CoupleDTO(10L, 1L, 2L, "CONNECTED"));
        when(goalMapper.selectGoalsByCoupleIdForUpdate(10L)).thenReturn(List.of(
                goal(1L, "기존 메인", "CONFIRMED", true),
                pendingGoal
        ));
        when(goalMapper.confirmGoal(2L)).thenReturn(1);

        GoalStatusUpdateResponseDTO response = service.decideMainGoalRequest(
                2L,
                2L,
                new GoalDecisionRequestDTO("APPROVE")
        );

        assertEquals("CONFIRMED", response.getStatus());
        assertEquals(true, response.getIsMain());
        verify(goalMapper).clearMainGoal(10L, 2L);
        verify(goalMapper).confirmGoal(2L);
    }

    @Test
    void decideMainGoalRequestRejectsPartnerRequest() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        GoalVO pendingGoal = goal(2L, "대기 예산", "PENDING", false);
        pendingGoal.setRequestedBy(1L);
        when(coupleService.getConnectedCoupleByUserId(2L))
                .thenReturn(new CoupleDTO(10L, 1L, 2L, "CONNECTED"));
        when(goalMapper.selectGoalsByCoupleIdForUpdate(10L)).thenReturn(List.of(pendingGoal));
        when(goalMapper.rejectGoalRequest(2L)).thenReturn(1);

        GoalStatusUpdateResponseDTO response = service.decideMainGoalRequest(
                2L,
                2L,
                new GoalDecisionRequestDTO("REJECT")
        );

        assertEquals("NORMAL", response.getStatus());
        assertEquals(false, response.getIsMain());
        verify(goalMapper).rejectGoalRequest(2L);
        verify(goalMapper, never()).confirmGoal(any());
    }

    @Test
    void decideMainGoalRequestRejectsRequesterDecision() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        GoalVO pendingGoal = goal(2L, "대기 예산", "PENDING", false);
        pendingGoal.setRequestedBy(1L);
        when(coupleService.getConnectedCoupleByUserId(1L))
                .thenReturn(new CoupleDTO(10L, 1L, 2L, "CONNECTED"));
        when(goalMapper.selectGoalsByCoupleIdForUpdate(10L)).thenReturn(List.of(pendingGoal));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.decideMainGoalRequest(
                        1L,
                        2L,
                        new GoalDecisionRequestDTO("APPROVE")
                )
        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        verify(goalMapper, never()).confirmGoal(any());
    }

    @Test
    void updateGoalReplacesGoalAndItemsWhileKeepingServerManagedValues() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        GoalVO existingGoal = goal(1L, "기존 예산", "CONFIRMED", true);
        when(coupleService.getConnectedCoupleByUserId(2L))
                .thenReturn(new CoupleDTO(10L, 1L, 2L, "CONNECTED"));
        when(goalMapper.selectGoalsByCoupleIdForUpdate(10L)).thenReturn(List.of(existingGoal));
        when(goalMapper.updateGoal(any(GoalVO.class))).thenReturn(1);
        when(goalMapper.insertGoalItems(eq(1L), anyList())).thenReturn(2);

        GoalUpdateResponseDTO response = service.updateGoal(2L, 1L, validUpdateRequest());

        assertEquals("수정된 균형형 결혼 예산", response.getName());
        assertEquals(35_000_000L, response.getTotalBudget());
        assertEquals("CONFIRMED", response.getStatus());
        assertEquals(true, response.getIsMain());
        assertEquals(existingGoal.getCreatedAt(), response.getCreatedAt());
        assertEquals(false, response.getItems().get(0).getCompleted());
        verify(goalMapper).deleteGoalItems(1L);
        verify(goalMapper).insertGoalItems(eq(1L), anyList());
    }

    @Test
    void deleteGoalDeletesNonMainGoalForConnectedCouple() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        when(coupleService.getConnectedCoupleByUserId(1L))
                .thenReturn(new CoupleDTO(10L, 1L, 2L, "CONNECTED"));
        when(goalMapper.selectGoalsByCoupleIdForUpdate(10L))
                .thenReturn(List.of(goal(2L, "일반 예산", "NORMAL", false)));
        when(goalMapper.deleteGoal(2L, 10L)).thenReturn(1);

        GoalDeleteResponseDTO response = service.deleteGoal(1L, 2L);

        assertEquals(2L, response.getGoalId());
        verify(goalMapper).deleteGoal(2L, 10L);
    }

    @Test
    void deleteGoalRejectsMainGoal() {
        GoalMapper goalMapper = mock(GoalMapper.class);
        CoupleService coupleService = mock(CoupleService.class);
        GoalService service = new GoalServiceImpl(goalMapper, coupleService);
        when(coupleService.getConnectedCoupleByUserId(1L))
                .thenReturn(new CoupleDTO(10L, 1L, 2L, "CONNECTED"));
        when(goalMapper.selectGoalsByCoupleIdForUpdate(10L))
                .thenReturn(List.of(goal(1L, "메인 예산", "CONFIRMED", true)));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.deleteGoal(1L, 1L)
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        assertEquals("메인 시안은 삭제할 수 없습니다.", exception.getMessage());
        verify(goalMapper, never()).deleteGoal(any(), any());
    }

    private GoalCreateRequestDTO validRequest() {
        return GoalCreateRequestDTO.builder()
                .name("균형형 결혼 예산")
                .weddingDate(LocalDate.of(2099, 10, 9))
                .region("서울(강남)")
                .groomRatio(50L)
                .brideRatio(50L)
                .items(validItems())
                .build();
    }

    private GoalUpdateRequestDTO validUpdateRequest() {
        return GoalUpdateRequestDTO.builder()
                .name("수정된 균형형 결혼 예산")
                .weddingDate(LocalDate.of(2099, 10, 9))
                .region("서울(강남)")
                .groomRatio(60L)
                .brideRatio(40L)
                .items(validItems())
                .build();
    }

    private List<GoalItemCreateRequestDTO> validItems() {
        return List.of(
                item("예식장", 18_000_000L, true),
                item("스튜디오", 17_000_000L, true)
        );
    }

    private GoalItemCreateRequestDTO item(
            String category,
            Long targetAmount,
            Boolean included
    ) {
        return GoalItemCreateRequestDTO.builder()
                .category(category)
                .targetAmount(targetAmount)
                .included(included)
                .type("균형형")
                .build();
    }

    private GoalVO goal(Long id, String name, String status, Boolean isMain) {
        return GoalVO.builder()
                .id(id)
                .coupleId(10L)
                .name(name)
                .weddingDate(LocalDate.of(2099, 10, 9))
                .region("서울(강남)")
                .totalBudget(35_000_000L)
                .groomRatio(50L)
                .brideRatio(50L)
                .status(status)
                .isMain(isMain)
                .createdAt(LocalDateTime.of(2026, 7, 30, 10, 30))
                .build();
    }

    private GoalItemVO goalItem(Long id, Long goalId, String category) {
        return GoalItemVO.builder()
                .id(id)
                .goalId(goalId)
                .category(category)
                .targetAmount(1_000_000L)
                .included(true)
                .completed(false)
                .type("균형형")
                .build();
    }
}
