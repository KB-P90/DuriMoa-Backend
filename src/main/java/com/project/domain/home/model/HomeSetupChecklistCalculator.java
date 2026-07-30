package com.project.domain.home.model;

import com.project.domain.home.model.HomeSetupChecklistResult.Status;
import com.project.domain.home.model.HomeSetupChecklistResult.Step;
import com.project.domain.home.model.HomeSetupChecklistResult.StepResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class HomeSetupChecklistCalculator {

    // 실제 수행 근거와 단계 순서를 기준으로 홈 체크리스트 상태를 계산합니다.
    public HomeSetupChecklistResult calculate(HomeSetupChecklistEvidence evidence) {
        HomeSetupChecklistEvidence safeEvidence = evidence == null ? HomeSetupChecklistEvidence.empty() : evidence;
        List<StepResult> stepResults = new ArrayList<>();

        // 앞 단계가 모두 완료되어 현재 단계 완료를 인정할 수 있는지 나타냅니다.
        Boolean previousStepsCompleted = true;
        // 첫 미완료 단계를 CURRENT로 지정했는지 추적합니다.
        Boolean currentStepAssigned = false;
        // 앞 단계부터 연속해서 완료된 단계 수입니다.
        Integer completedCount = 0;
        // 처음으로 완료되지 않은 단계 번호입니다.
        Integer currentStep = null;

        for (Step step : Step.values()) {
            // 현재 단계의 실제 도메인 수행 여부입니다.
            Boolean rawCompleted = isRawCompleted(step, safeEvidence);
            // 앞 단계까지 완료된 경우에만 현재 단계 완료를 유효하게 인정합니다.
            Boolean validCompleted = Boolean.TRUE.equals(previousStepsCompleted)
                    && Boolean.TRUE.equals(rawCompleted);

            Status status;
            if (Boolean.TRUE.equals(validCompleted)) {
                status = Status.COMPLETED;
                completedCount++;
            } else if (!Boolean.TRUE.equals(currentStepAssigned)) {
                status = Status.CURRENT;
                currentStep = step.getStep();
                currentStepAssigned = true;
            } else {
                status = Status.LOCKED;
            }

            stepResults.add(StepResult.builder()
                    .step(step)
                    .completed(validCompleted)
                    .status(status)
                    .build());
            previousStepsCompleted = validCompleted;
        }

        return HomeSetupChecklistResult.builder()
                .totalCount(Step.values().length)
                .completedCount(completedCount)
                .currentStep(currentStep)
                .steps(stepResults)
                .build();
    }

    // 단계별 원시 완료 근거를 조회합니다.
    private Boolean isRawCompleted(Step step, HomeSetupChecklistEvidence evidence) {
        switch (step) {
            case COUPLE_CONNECTED:
                return evidence.getCoupleConnected();
            case ACCOUNT_LINKED:
                return evidence.getAccountLinked();
            case WEDDING_GOAL_CREATED:
                return evidence.getWeddingGoalCreated();
            case REGION_SELECTED:
                return evidence.getRegionSelected();
            case FIRST_SAVING_RECORDED:
                return evidence.getFirstSavingRecorded();
            default:
                return false;
        }
    }
}
