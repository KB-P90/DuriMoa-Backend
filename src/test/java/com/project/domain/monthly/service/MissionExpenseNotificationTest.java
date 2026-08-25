package com.project.domain.monthly.service;

import com.project.common.enums.RecordType;
import com.project.domain.calendar.domain.ServiceRecord;
import com.project.domain.monthly.dto.SavingMissionDTO;
import com.project.domain.monthly.mapper.MonthlyMapper;
import com.project.domain.notification.domain.NotificationType;
import com.project.domain.notification.dto.NotificationCreateRequestDTO;
import com.project.domain.notification.service.NotificationService;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MissionExpenseNotificationTest {

    @Mock
    private MonthlyMapper monthlyMapper;

    @Mock
    private NotificationService notificationService;

    @Test
    void sendsOneNotificationPerMatchedInProgressCategory() {
        MonthlyServiceImpl monthlyService = new MonthlyServiceImpl(monthlyMapper, notificationService);

        Mockito.when(monthlyMapper.selectInProgressSavingMissions(1L))
                .thenReturn(List.of(
                        SavingMissionDTO.builder()
                                .missionId(101L)
                                .categoryCode("FOOD")
                                .title("월 8회에서 5회로 줄이면")
                                .status("도전중")
                                .build(),
                        SavingMissionDTO.builder()
                                .missionId(102L)
                                .categoryCode("SHOPPING")
                                .title("월 6회에서 4회로 줄이면")
                                .status("도전중")
                                .build()));
        Mockito.when(notificationService.existsNotificationToday(
                        Mockito.eq(1L), Mockito.eq(NotificationType.SAVING_MISSION_EXPENSE_ALERT), Mockito.anyLong()))
                .thenReturn(false);

        List<ServiceRecord> todayExpenses = List.of(
                ServiceRecord.builder().userId(1L).type(RecordType.EXPENSE).amount(4500L).category("FOOD").build(),
                ServiceRecord.builder().userId(1L).type(RecordType.EXPENSE).amount(12000L).category("SHOPPING").build(),
                ServiceRecord.builder().userId(1L).type(RecordType.EXPENSE).amount(9000L).category("CULTURE").build());

        monthlyService.notifyInProgressMissionExpenses(1L, todayExpenses);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        Mockito.verify(notificationService, Mockito.times(2)).createNotification(captor.capture());

        List<NotificationCreateRequestDTO> requests = captor.getAllValues();
        Assertions.assertTrue(requests.stream().anyMatch(r ->
                r.getTargetId().equals(101L)
                        && r.getContent().equals("오늘 식비 지출이 발생했어요 (도전 중: 월 8회에서 5회)")));
        Assertions.assertTrue(requests.stream().anyMatch(r ->
                r.getTargetId().equals(102L)
                        && r.getContent().equals("오늘 쇼핑 지출이 발생했어요 (도전 중: 월 6회에서 4회)")));
    }

    @Test
    void skipsCategoriesWithNoInProgressMission() {
        MonthlyServiceImpl monthlyService = new MonthlyServiceImpl(monthlyMapper, notificationService);

        Mockito.when(monthlyMapper.selectInProgressSavingMissions(1L))
                .thenReturn(List.of(
                        SavingMissionDTO.builder()
                                .missionId(101L)
                                .categoryCode("FOOD")
                                .title("월 8회에서 5회로 줄이면")
                                .status("도전중")
                                .build()));

        List<ServiceRecord> todayExpenses = List.of(
                ServiceRecord.builder().userId(1L).type(RecordType.EXPENSE).amount(9000L).category("CULTURE").build());

        monthlyService.notifyInProgressMissionExpenses(1L, todayExpenses);

        Mockito.verify(notificationService, Mockito.never()).createNotification(Mockito.any());
    }

    @Test
    void skipsAlreadyNotifiedMissionToday() {
        MonthlyServiceImpl monthlyService = new MonthlyServiceImpl(monthlyMapper, notificationService);

        Mockito.when(monthlyMapper.selectInProgressSavingMissions(1L))
                .thenReturn(List.of(
                        SavingMissionDTO.builder()
                                .missionId(101L)
                                .categoryCode("FOOD")
                                .title("월 8회에서 5회로 줄이면")
                                .status("도전중")
                                .build()));
        Mockito.when(notificationService.existsNotificationToday(1L, NotificationType.SAVING_MISSION_EXPENSE_ALERT, 101L))
                .thenReturn(true);

        List<ServiceRecord> todayExpenses = List.of(
                ServiceRecord.builder().userId(1L).type(RecordType.EXPENSE).amount(4500L).category("FOOD").build());

        monthlyService.notifyInProgressMissionExpenses(1L, todayExpenses);

        Mockito.verify(notificationService, Mockito.never()).createNotification(Mockito.any());
    }
}
