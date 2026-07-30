package com.project.domain.home.service;

import com.project.domain.home.dto.HomeResponseDTO;
import com.project.domain.home.dto.HomeResponseDTO.AssetSummaryResponseDTO;
import com.project.domain.home.dto.HomeResponseDTO.CoupleResponseDTO;
import com.project.domain.home.dto.HomeResponseDTO.GoalProgressResponseDTO;
import com.project.domain.home.dto.HomeResponseDTO.GoalSummaryResponseDTO;
import com.project.domain.home.dto.HomeResponseDTO.MissionResponseDTO;
import com.project.domain.home.dto.HomeResponseDTO.SavingAlertResponseDTO;
import com.project.domain.home.dto.HomeResponseDTO.SavingMissionSummaryResponseDTO;
import com.project.domain.home.dto.HomeResponseDTO.SetupChecklistResponseDTO;
import com.project.domain.home.dto.HomeResponseDTO.SetupChecklistStepResponseDTO;
import com.project.domain.home.exception.HomeException.HomeAuthenticationRequiredException;
import com.project.domain.home.exception.HomeException.HomeUserNotFoundException;
import com.project.domain.home.mapper.HomeMapper;
import com.project.domain.home.model.HomeRecord.HomeChecklistEvidenceRecord;
import com.project.domain.home.model.HomeRecord.HomeCoupleRecord;
import com.project.domain.home.model.HomeRecord.HomeSavingMissionSummaryRecord;
import com.project.domain.home.model.HomeRecord.HomeUserRecord;
import com.project.domain.home.model.HomeSetupChecklistCalculator;
import com.project.domain.home.model.HomeSetupChecklistEvidence;
import com.project.domain.home.model.HomeSetupChecklistResult;
import com.project.domain.home.model.HomeSetupChecklistResult.StepResult;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private static final String MONTHLY_EXPENSE_NAVIGATION_MESSAGE = "월별지출관리 페이지로 이동";

    // 홈 조회에 필요한 최소 DB 조회를 담당하는 Mapper입니다.
    private final HomeMapper homeMapper;

    // 실제 수행 근거를 5단계 체크리스트 상태로 변환하는 계산 객체입니다.
    private final HomeSetupChecklistCalculator checklistCalculator;

    // 인증 사용자의 홈 화면 응답 뼈대와 체크리스트를 조회합니다.
    @Override
    @Transactional(readOnly = true)
    public HomeResponseDTO getHome(Long authenticatedUserId) {
        validateAuthenticatedUser(authenticatedUserId);

        HomeUserRecord user = homeMapper.selectHomeUser(authenticatedUserId);
        if (user == null) {
            throw new HomeUserNotFoundException();
        }

        HomeCoupleRecord couple = homeMapper.selectActiveCouple(authenticatedUserId);
        HomeChecklistEvidenceRecord evidenceRecord = selectEvidence(authenticatedUserId, couple);
        HomeSetupChecklistEvidence evidence = buildChecklistEvidence(couple, evidenceRecord);
        HomeSetupChecklistResult checklistResult = checklistCalculator.calculate(evidence);
        HomeSavingMissionSummaryRecord savingMissionSummary = selectSavingMissionSummary(authenticatedUserId);

        return HomeResponseDTO.builder()
                .couple(buildCoupleResponse(user, couple, evidenceRecord))
                .assetSummary(buildAssetSummaryPlaceholder())
                .savingAlert(buildSavingAlertPlaceholder())
                .savingMissionSummary(buildSavingMissionSummary(savingMissionSummary))
                .goalSummary(buildGoalSummary(evidenceRecord))
                .setupChecklist(buildSetupChecklistResponse(checklistResult))
                .build();
    }

    // 인증 사용자 ID가 홈 조회에 사용할 수 있는 값인지 확인합니다.
    private void validateAuthenticatedUser(Long authenticatedUserId) {
        if (authenticatedUserId == null || authenticatedUserId <= 0) {
            throw new HomeAuthenticationRequiredException();
        }
    }

    // 활성 커플이 있을 때만 뒤 단계 완료 근거를 조회합니다.
    private HomeChecklistEvidenceRecord selectEvidence(Long authenticatedUserId, HomeCoupleRecord couple) {
        if (couple == null) {
            return null;
        }
        return homeMapper.selectChecklistEvidence(
                authenticatedUserId,
                couple.getCoupleId(),
                couple.getGroomId(),
                couple.getBrideId()
        );
    }

    // DB 조회 결과를 체크리스트 원시 완료 근거로 변환합니다.
    private HomeSetupChecklistEvidence buildChecklistEvidence(
            HomeCoupleRecord couple,
            HomeChecklistEvidenceRecord evidenceRecord
    ) {
        if (couple == null) {
            return HomeSetupChecklistEvidence.empty();
        }

        return HomeSetupChecklistEvidence.builder()
                .coupleConnected(true)
                .accountLinked(evidenceRecord != null && Boolean.TRUE.equals(evidenceRecord.getAccountLinked()))
                .weddingGoalCreated(evidenceRecord != null
                        && Boolean.TRUE.equals(evidenceRecord.getWeddingGoalCreated()))
                .regionSelected(evidenceRecord != null && Boolean.TRUE.equals(evidenceRecord.getRegionSelected()))
                .firstSavingRecorded(evidenceRecord != null
                        && Boolean.TRUE.equals(evidenceRecord.getFirstSavingRecorded()))
                .build();
    }

    // 사용자와 커플 연결 정보를 홈 상단 응답으로 변환합니다.
    private CoupleResponseDTO buildCoupleResponse(
            HomeUserRecord user,
            HomeCoupleRecord couple,
            HomeChecklistEvidenceRecord evidenceRecord
    ) {
        if (couple == null) {
            return CoupleResponseDTO.builder()
                    .userId(user.getUserId())
                    .userName(user.getUserName())
                    .userRole(user.getUserRole())
                    .build();
        }

        return CoupleResponseDTO.builder()
                .coupleId(couple.getCoupleId())
                .userId(couple.getUserId())
                .userName(couple.getUserName())
                .userRole(couple.getUserRole())
                .partnerUserId(couple.getPartnerUserId())
                .partnerName(couple.getPartnerName())
                .partnerRole(couple.getPartnerRole())
                .coupleStatus(couple.getCoupleStatus())
                .goalId(evidenceRecord == null ? null : evidenceRecord.getMainGoalId())
                .weddingDate(evidenceRecord == null ? null : evidenceRecord.getWeddingDate())
                .remainingMonths(null)
                .budgetPlanId(null)
                .build();
    }

    // TODO: 자산·지출 제공자의 총예산·누적자금·지출·잔여금 조회 계약이 생기면 연결합니다.
    private AssetSummaryResponseDTO buildAssetSummaryPlaceholder() {
        return AssetSummaryResponseDTO.builder().build();
    }

    // TODO: 알림 제공자의 목표 변경 알림 조회 계약이 생기면 연결합니다.
    private SavingAlertResponseDTO buildSavingAlertPlaceholder() {
        return SavingAlertResponseDTO.builder().build();
    }

    // 홈에서 보여줄 현재 월 도전중 절약미션 요약을 조회합니다.
    private HomeSavingMissionSummaryRecord selectSavingMissionSummary(Long authenticatedUserId) {
        YearMonth currentYearMonth = YearMonth.now();
        return homeMapper.selectSavingMissionSummary(
                authenticatedUserId,
                currentYearMonth.getYear(),
                currentYearMonth.getMonthValue()
        );
    }

    // 도전중 절약미션 테이블 조회 결과를 홈 요약 응답으로 변환합니다.
    private SavingMissionSummaryResponseDTO buildSavingMissionSummary(
            HomeSavingMissionSummaryRecord summaryRecord
    ) {
        return SavingMissionSummaryResponseDTO.builder()
                .totalExpectedSavingAmount(summaryRecord == null
                        || summaryRecord.getTotalExpectedSavingAmount() == null
                        ? 0L
                        : summaryRecord.getTotalExpectedSavingAmount())
                .inProgressMissionCount(summaryRecord == null
                        || summaryRecord.getInProgressMissionCount() == null
                        ? 0
                        : summaryRecord.getInProgressMissionCount())
                .completedMissionCount(summaryRecord == null
                        || summaryRecord.getCompletedMissionCount() == null
                        ? 0
                        : summaryRecord.getCompletedMissionCount())
                .mission(buildMissionResponse(summaryRecord))
                .navigationMessage(MONTHLY_EXPENSE_NAVIGATION_MESSAGE)
                .build();
    }

    // 홈에 대표로 보여줄 절약미션이 없으면 null을 반환합니다.
    private MissionResponseDTO buildMissionResponse(HomeSavingMissionSummaryRecord summaryRecord) {
        if (summaryRecord == null || summaryRecord.getMissionId() == null) {
            return null;
        }

        return MissionResponseDTO.builder()
                .missionId(summaryRecord.getMissionId())
                .categoryCode(summaryRecord.getCategoryCode())
                .title(summaryRecord.getTitle())
                .status(summaryRecord.getStatus())
                .expectedSavingAmount(summaryRecord.getExpectedSavingAmount())
                .build();
    }

    // 목표 요약은 목표 존재 여부만 연결하고 금액·달성률은 계산하지 않습니다.
    private GoalSummaryResponseDTO buildGoalSummary(HomeChecklistEvidenceRecord evidenceRecord) {
        Boolean hasJointGoal = evidenceRecord != null
                && Boolean.TRUE.equals(evidenceRecord.getWeddingGoalCreated());

        return GoalSummaryResponseDTO.builder()
                .jointGoal(GoalProgressResponseDTO.builder()
                        .goalId(hasJointGoal ? evidenceRecord.getMainGoalId() : null)
                        .status(hasJointGoal ? evidenceRecord.getMainGoalStatus() : null)
                        .targetAmount(null)
                        .currentAmount(null)
                        .achievementRate(null)
                        .setupRequired(!Boolean.TRUE.equals(hasJointGoal))
                        .build())
                .personalGoal(GoalProgressResponseDTO.builder().build())
                .build();
    }

    // 계산 결과를 API 응답 DTO로 변환합니다.
    private SetupChecklistResponseDTO buildSetupChecklistResponse(HomeSetupChecklistResult checklistResult) {
        List<SetupChecklistStepResponseDTO> steps = checklistResult.getSteps().stream()
                .map(this::buildSetupChecklistStepResponse)
                .collect(Collectors.toList());

        return SetupChecklistResponseDTO.builder()
                .totalCount(checklistResult.getTotalCount())
                .completedCount(checklistResult.getCompletedCount())
                .currentStep(checklistResult.getCurrentStep())
                .steps(steps)
                .build();
    }

    // 단계별 계산 결과를 API 응답 DTO로 변환합니다.
    private SetupChecklistStepResponseDTO buildSetupChecklistStepResponse(StepResult stepResult) {
        return SetupChecklistStepResponseDTO.builder()
                .step(stepResult.getStep().getStep())
                .code(stepResult.getStep().getCode())
                .title(stepResult.getStep().getTitle())
                .completed(stepResult.getCompleted())
                .status(stepResult.getStatus().name())
                .build();
    }
}
