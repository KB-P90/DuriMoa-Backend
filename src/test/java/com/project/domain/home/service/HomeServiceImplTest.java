package com.project.domain.home.service;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.home.dto.HomeResponseDTO;
import com.project.domain.home.mapper.HomeMapper;
import com.project.domain.home.model.HomeRecord.HomeAssetRecord;
import com.project.domain.home.model.HomeRecord.HomeAssetSummaryRecord;
import com.project.domain.home.model.HomeRecord.HomeBudgetPreviewRecord;
import com.project.domain.home.model.HomeRecord.HomeChecklistEvidenceRecord;
import com.project.domain.home.model.HomeRecord.HomeCoupleRecord;
import com.project.domain.home.model.HomeRecord.HomeSavingMissionSummaryRecord;
import com.project.domain.home.model.HomeRecord.HomeUserRecord;
import com.project.domain.home.model.HomeSetupChecklistCalculator;
import java.util.List;
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
        Mockito.when(mapper.selectAccountLinked(1L)).thenReturn(false);
        Mockito.when(mapper.selectExpectedBudgetPreviewRegions()).thenReturn(budgetPreviewRegions());

        HomeResponseDTO response = service.getHome(1L);

        Assertions.assertEquals(0, response.getSetupChecklist().getCompletedCount());
        Assertions.assertEquals(1, response.getSetupChecklist().getCurrentStep());
        Assertions.assertFalse(response.getSetupCompleted());
        Assertions.assertNotNull(response.getExpectedBudgetPreview());
        Assertions.assertEquals("균형형", response.getExpectedBudgetPreview().getBudgetType());
        Assertions.assertEquals("MID_50", response.getExpectedBudgetPreview().getAmountBasis());
        Assertions.assertEquals(1, response.getExpectedBudgetPreview().getRegions().size());
        Assertions.assertEquals("서울", response.getExpectedBudgetPreview().getRegions().get(0).getDisplayRegion());
        Assertions.assertEquals(11_790_000L,
                response.getExpectedBudgetPreview().getRegions().get(0).getBalancedAmount());
        Assertions.assertNull(response.getAssetSummary());
        Assertions.assertNull(response.getGoalSummary());
        Assertions.assertNull(response.getSavingMissionSummary());
        Assertions.assertEquals(1L, response.getCouple().getUserId());
        Assertions.assertNull(response.getCouple().getCoupleId());
        Mockito.verify(mapper, Mockito.never()).selectChecklistEvidence(
                Mockito.anyLong(),
                Mockito.anyLong(),
                Mockito.anyLong(),
                Mockito.anyLong()
        );
        Mockito.verify(mapper, Mockito.never()).selectSavingMissionSummary(
                Mockito.anyLong(),
                Mockito.anyInt(),
                Mockito.anyInt()
        );
    }

    // 계좌 연결만 완료된 사용자는 커플 연결 단계가 현재 단계가 됩니다.
    @Test
    void getHomeMovesCurrentStepToCoupleWhenOnlyAccountLinked() {
        HomeMapper mapper = Mockito.mock(HomeMapper.class);
        HomeServiceImpl service = new HomeServiceImpl(mapper, new HomeSetupChecklistCalculator());

        Mockito.when(mapper.selectHomeUser(1L)).thenReturn(user(1L));
        Mockito.when(mapper.selectActiveCouple(1L)).thenReturn(null);
        Mockito.when(mapper.selectAccountLinked(1L)).thenReturn(true);
        Mockito.when(mapper.selectExpectedBudgetPreviewRegions()).thenReturn(budgetPreviewRegions());

        HomeResponseDTO response = service.getHome(1L);

        Assertions.assertEquals(1, response.getSetupChecklist().getCompletedCount());
        Assertions.assertEquals(2, response.getSetupChecklist().getCurrentStep());
        Assertions.assertFalse(response.getSetupCompleted());
        Assertions.assertEquals("ACCOUNT_LINKED", response.getSetupChecklist().getSteps().get(0).getCode());
        Assertions.assertEquals("COUPLE_CONNECTED", response.getSetupChecklist().getSteps().get(1).getCode());
    }

    // 실제 수행 근거가 모두 있으면 다섯 단계가 모두 완료됩니다.
    @Test
    void getHomeReturnsCompletedChecklistWhenAllEvidenceExists() {
        HomeMapper mapper = Mockito.mock(HomeMapper.class);
        HomeServiceImpl service = new HomeServiceImpl(mapper, new HomeSetupChecklistCalculator());

        Mockito.when(mapper.selectHomeUser(1L)).thenReturn(user(1L));
        Mockito.when(mapper.selectActiveCouple(1L)).thenReturn(couple());
        Mockito.when(mapper.selectAccountLinked(1L)).thenReturn(true);
        Mockito.when(mapper.selectChecklistEvidence(1L, 100L, 1L, 2L)).thenReturn(fullEvidence());
        Mockito.when(mapper.selectAssetSummary(1L, 1L, 2L, 30L)).thenReturn(assetSummary());
        Mockito.when(mapper.selectAssetRecords(1L, 2L)).thenReturn(List.of(
                assetRecord(1L, "SAVING", "WEDDINGSAVING", 11_900_000L, "ACCOUNT"),
                assetRecord(2L, "SAVING", "결혼 저축", 19_290_000L, "CARD"),
                assetRecord(1L, "EXPENSE", "WEDDINGHALL", 1_000_000L, "SERVICE")
        ));
        Mockito.when(mapper.countUnreadNotifications(1L)).thenReturn(2);
        Mockito.when(mapper.selectSavingMissionSummary(
                Mockito.eq(1L),
                Mockito.anyInt(),
                Mockito.anyInt()
        )).thenReturn(savingMissionSummary());

        HomeResponseDTO response = service.getHome(1L);

        Assertions.assertEquals(5, response.getSetupChecklist().getCompletedCount());
        Assertions.assertNull(response.getSetupChecklist().getCurrentStep());
        Assertions.assertTrue(response.getSetupCompleted());
        Assertions.assertNull(response.getExpectedBudgetPreview());
        Assertions.assertEquals(30L, response.getCouple().getGoalId());
        Assertions.assertEquals("2026-12-20", response.getCouple().getWeddingDate());
        Assertions.assertEquals(30L, response.getGoalSummary().getJointGoal().getGoalId());
        Assertions.assertFalse(response.getGoalSummary().getJointGoal().getSetupRequired());
        Assertions.assertEquals(50_000_000L, response.getAssetSummary().getTotalBudgetAmount());
        Assertions.assertEquals(31_190_000L, response.getAssetSummary().getTotalSavedAmount());
        Assertions.assertEquals(18_810_000L, response.getAssetSummary().getRemainingWeddingFundAmount());
        Assertions.assertEquals(31_190_000L, response.getGoalSummary().getJointGoal().getCurrentAmount());
        Assertions.assertEquals(62, response.getGoalSummary().getJointGoal().getAchievementRate());
        Assertions.assertEquals(2, response.getNotificationSummary().getUnreadCount());
        Assertions.assertTrue(response.getNotificationSummary().getHasUnread());
        Assertions.assertEquals(30_000_000L, response.getGoalSummary().getPersonalGoal().getTargetAmount());
        Assertions.assertEquals(11_900_000L, response.getGoalSummary().getPersonalGoal().getCurrentAmount());
        Assertions.assertEquals(39, response.getGoalSummary().getPersonalGoal().getAchievementRate());
        Assertions.assertEquals(70000L, response.getSavingMissionSummary().getTotalExpectedSavingAmount());
        Assertions.assertEquals(1, response.getSavingMissionSummary().getInProgressMissionCount());
        Assertions.assertEquals(1, response.getSavingMissionSummary().getCompletedMissionCount());
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

        CustomException exception = Assertions.assertThrows(
                CustomException.class,
                () -> service.getHome(99L)
        );
        Assertions.assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
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
        evidence.setWeddingGoalCreated(true);
        evidence.setMainPlanSelected(true);
        evidence.setMainGoalId(30L);
        evidence.setMainGoalStatus("CONFIRMED");
        evidence.setWeddingDate("2026-12-20");
        evidence.setRegion("서울");
        return evidence;
    }

    // 미완료 홈의 예상 예산 미리보기 원본 통계 데이터를 만듭니다.
    private List<HomeBudgetPreviewRecord> budgetPreviewRegions() {
        return List.of(
                budgetPreviewRegion("예식장", 5_000_000L),
                budgetPreviewRegion("스튜디오", 3_000_000L),
                budgetPreviewRegion("드레스", 2_000_000L),
                budgetPreviewRegion("메이크업", 1_790_000L)
        );
    }

    // 미완료 홈의 예상 예산 미리보기 원본 통계 단건을 만듭니다.
    private HomeBudgetPreviewRecord budgetPreviewRegion(String category, Long balancedAmount) {
        HomeBudgetPreviewRecord record = new HomeBudgetPreviewRecord();
        record.setRegion("서울(강남)");
        record.setCategory(category);
        record.setBalancedAmount(balancedAmount);
        return record;
    }

    // 메인 홈의 자산 요약 테스트 데이터를 만듭니다.
    private HomeAssetSummaryRecord assetSummary() {
        HomeAssetSummaryRecord summary = new HomeAssetSummaryRecord();
        summary.setTotalBudgetAmount(50_000_000L);
        summary.setPersonalTargetAmount(30_000_000L);
        summary.setBaseDate("2026-08-03");
        return summary;
    }

    // 홈 자산 요약 테스트용 거래 원본 데이터를 만듭니다.
    private HomeAssetRecord assetRecord(
            Long userId,
            String recordType,
            String category,
            Long amount,
            String source
    ) {
        HomeAssetRecord record = new HomeAssetRecord();
        record.setUserId(userId);
        record.setRecordType(recordType);
        record.setCategory(category);
        record.setAmount(amount);
        record.setSource(source);
        return record;
    }

    // 홈 도전중 절약미션 표시와 완료 미션 수 요약 테스트 데이터를 만듭니다.
    private HomeSavingMissionSummaryRecord savingMissionSummary() {
        HomeSavingMissionSummaryRecord summary = new HomeSavingMissionSummaryRecord();
        summary.setTotalExpectedSavingAmount(70000L);
        summary.setInProgressMissionCount(1);
        summary.setCompletedMissionCount(1);
        summary.setMissionId(10L);
        summary.setCategoryCode("CAFE");
        summary.setTitle("카페 지출 줄이기");
        summary.setStatus("도전중");
        summary.setExpectedSavingAmount(30000L);
        return summary;
    }
}
