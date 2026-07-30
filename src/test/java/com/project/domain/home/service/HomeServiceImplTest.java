package com.project.domain.home.service;

import com.project.domain.home.dto.HomeResponseDTO;
import com.project.domain.home.exception.HomeException.HomeUserNotFoundException;
import com.project.domain.home.mapper.HomeMapper;
import com.project.domain.home.model.HomeRecord.HomeChecklistEvidenceRecord;
import com.project.domain.home.model.HomeRecord.HomeCoupleRecord;
import com.project.domain.home.model.HomeRecord.HomeSavingMissionSummaryRecord;
import com.project.domain.home.model.HomeRecord.HomeUserRecord;
import com.project.domain.home.model.HomeSetupChecklistCalculator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomeServiceImplTest {

    // 활성 커플이 없으면 체크리스트 1단계가 현재 단계가 됩니다.
    @Test
    void getHomeReturnsChecklistWhenCoupleDoesNotExist() {
        HomeMapper mapper = Mockito.mock(HomeMapper.class);
        HomeServiceImpl service = new HomeServiceImpl(mapper, new HomeSetupChecklistCalculator());

        Mockito.when(mapper.selectHomeUser(1L)).thenReturn(user(1L));
        Mockito.when(mapper.selectActiveCouple(1L)).thenReturn(null);

        HomeResponseDTO response = service.getHome(1L);

        Assertions.assertEquals(0, response.getSetupChecklist().getCompletedCount());
        Assertions.assertEquals(1, response.getSetupChecklist().getCurrentStep());
        Assertions.assertEquals(1L, response.getCouple().getUserId());
        Assertions.assertNull(response.getCouple().getCoupleId());
        Assertions.assertEquals(0L, response.getSavingMissionSummary().getTotalExpectedSavingAmount());
        Assertions.assertEquals("월별지출관리 페이지로 이동",
                response.getSavingMissionSummary().getNavigationMessage());
        Mockito.verify(mapper, Mockito.never()).selectChecklistEvidence(
                Mockito.anyLong(),
                Mockito.anyLong(),
                Mockito.anyLong(),
                Mockito.anyLong()
        );
    }

    // 실제 수행 근거가 모두 있으면 다섯 단계가 모두 완료됩니다.
    @Test
    void getHomeReturnsCompletedChecklistWhenAllEvidenceExists() {
        HomeMapper mapper = Mockito.mock(HomeMapper.class);
        HomeServiceImpl service = new HomeServiceImpl(mapper, new HomeSetupChecklistCalculator());

        Mockito.when(mapper.selectHomeUser(1L)).thenReturn(user(1L));
        Mockito.when(mapper.selectActiveCouple(1L)).thenReturn(couple());
        Mockito.when(mapper.selectChecklistEvidence(1L, 100L, 1L, 2L)).thenReturn(fullEvidence());
        Mockito.when(mapper.selectSavingMissionSummary(
                Mockito.eq(1L),
                Mockito.anyInt(),
                Mockito.anyInt()
        )).thenReturn(savingMissionSummary());

        HomeResponseDTO response = service.getHome(1L);

        Assertions.assertEquals(5, response.getSetupChecklist().getCompletedCount());
        Assertions.assertNull(response.getSetupChecklist().getCurrentStep());
        Assertions.assertEquals(30L, response.getCouple().getGoalId());
        Assertions.assertEquals("2026-12-20", response.getCouple().getWeddingDate());
        Assertions.assertEquals(30L, response.getGoalSummary().getJointGoal().getGoalId());
        Assertions.assertFalse(response.getGoalSummary().getJointGoal().getSetupRequired());
        Assertions.assertNull(response.getAssetSummary().getTotalSavedAmount());
        Assertions.assertEquals(70000L, response.getSavingMissionSummary().getTotalExpectedSavingAmount());
        Assertions.assertEquals(1, response.getSavingMissionSummary().getInProgressMissionCount());
        Assertions.assertEquals(0, response.getSavingMissionSummary().getCompletedMissionCount());
        Assertions.assertEquals(10L, response.getSavingMissionSummary().getMission().getMissionId());
        Assertions.assertEquals("카페 지출 줄이기", response.getSavingMissionSummary().getMission().getTitle());
        Assertions.assertEquals("도전중", response.getSavingMissionSummary().getMission().getStatus());
    }

    // 인증 사용자 ID에 해당하는 사용자가 없으면 홈 조회를 거부합니다.
    @Test
    void getHomeThrowsWhenUserDoesNotExist() {
        HomeMapper mapper = Mockito.mock(HomeMapper.class);
        HomeServiceImpl service = new HomeServiceImpl(mapper, new HomeSetupChecklistCalculator());

        Mockito.when(mapper.selectHomeUser(99L)).thenReturn(null);

        Assertions.assertThrows(HomeUserNotFoundException.class, () -> service.getHome(99L));
    }

    // 사용자 기본 정보 테스트 데이터를 만듭니다.
    private HomeUserRecord user(Long userId) {
        HomeUserRecord user = new HomeUserRecord();
        user.setUserId(userId);
        user.setUserName("박기정");
        user.setUserRole("G");
        return user;
    }

    // 활성 커플 테스트 데이터를 만듭니다.
    private HomeCoupleRecord couple() {
        HomeCoupleRecord couple = new HomeCoupleRecord();
        couple.setCoupleId(100L);
        couple.setGroomId(1L);
        couple.setBrideId(2L);
        couple.setUserId(1L);
        couple.setUserName("박기정");
        couple.setUserRole("G");
        couple.setPartnerUserId(2L);
        couple.setPartnerName("김두리");
        couple.setPartnerRole("B");
        couple.setCoupleStatus("CONNECTED");
        return couple;
    }

    // 전체 완료 상태의 체크리스트 근거 테스트 데이터를 만듭니다.
    private HomeChecklistEvidenceRecord fullEvidence() {
        HomeChecklistEvidenceRecord evidence = new HomeChecklistEvidenceRecord();
        evidence.setAccountLinked(true);
        evidence.setWeddingGoalCreated(true);
        evidence.setRegionSelected(true);
        evidence.setFirstSavingRecorded(true);
        evidence.setMainGoalId(30L);
        evidence.setMainGoalStatus("CONFIRMED");
        evidence.setWeddingDate("2026-12-20");
        evidence.setRegion("서울");
        return evidence;
    }

    // 홈 도전중 절약미션 요약 테스트 데이터를 만듭니다.
    private HomeSavingMissionSummaryRecord savingMissionSummary() {
        HomeSavingMissionSummaryRecord summary = new HomeSavingMissionSummaryRecord();
        summary.setTotalExpectedSavingAmount(70000L);
        summary.setInProgressMissionCount(1);
        summary.setCompletedMissionCount(0);
        summary.setMissionId(10L);
        summary.setCategoryCode("CAFE");
        summary.setTitle("카페 지출 줄이기");
        summary.setStatus("도전중");
        summary.setExpectedSavingAmount(30000L);
        return summary;
    }
}
