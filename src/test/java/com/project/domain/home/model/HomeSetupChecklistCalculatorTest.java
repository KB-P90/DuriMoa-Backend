package com.project.domain.home.model;

import com.project.domain.home.model.HomeSetupChecklistResult.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HomeSetupChecklistCalculatorTest {

    // 아무 단계도 완료되지 않았을 때 1단계만 현재 단계로 표시합니다.
    @Test
    void calculateReturnsFirstStepCurrentWhenNoEvidenceExists() {
        HomeSetupChecklistCalculator calculator = new HomeSetupChecklistCalculator();

        HomeSetupChecklistResult result = calculator.calculate(HomeSetupChecklistEvidence.empty());

        Assertions.assertEquals(5, result.getTotalCount());
        Assertions.assertEquals(0, result.getCompletedCount());
        Assertions.assertEquals(1, result.getCurrentStep());
        Assertions.assertEquals(Status.CURRENT, result.getSteps().get(0).getStatus());
        Assertions.assertEquals(Status.LOCKED, result.getSteps().get(1).getStatus());
    }

    // 커플 연결만 완료되면 계좌 연결 단계가 현재 단계가 됩니다.
    @Test
    void calculateMovesCurrentStepAfterCoupleConnected() {
        HomeSetupChecklistCalculator calculator = new HomeSetupChecklistCalculator();

        HomeSetupChecklistResult result = calculator.calculate(HomeSetupChecklistEvidence.builder()
                .coupleConnected(true)
                .accountLinked(false)
                .weddingGoalCreated(false)
                .regionSelected(false)
                .firstSavingRecorded(false)
                .build());

        Assertions.assertEquals(1, result.getCompletedCount());
        Assertions.assertEquals(2, result.getCurrentStep());
        Assertions.assertTrue(result.getSteps().get(0).getCompleted());
        Assertions.assertEquals(Status.CURRENT, result.getSteps().get(1).getStatus());
    }

    // 앞 단계가 미완료이면 뒤 단계 원시 데이터가 있어도 잠금 상태로 둡니다.
    @Test
    void calculateLocksLaterStepsWhenPreviousStepIsIncomplete() {
        HomeSetupChecklistCalculator calculator = new HomeSetupChecklistCalculator();

        HomeSetupChecklistResult result = calculator.calculate(HomeSetupChecklistEvidence.builder()
                .coupleConnected(true)
                .accountLinked(false)
                .weddingGoalCreated(true)
                .regionSelected(true)
                .firstSavingRecorded(true)
                .build());

        Assertions.assertEquals(1, result.getCompletedCount());
        Assertions.assertEquals(2, result.getCurrentStep());
        Assertions.assertFalse(result.getSteps().get(2).getCompleted());
        Assertions.assertEquals(Status.LOCKED, result.getSteps().get(2).getStatus());
        Assertions.assertEquals(Status.LOCKED, result.getSteps().get(4).getStatus());
    }

    // 다섯 단계가 모두 완료되면 현재 단계가 없습니다.
    @Test
    void calculateReturnsNullCurrentStepWhenAllStepsCompleted() {
        HomeSetupChecklistCalculator calculator = new HomeSetupChecklistCalculator();

        HomeSetupChecklistResult result = calculator.calculate(HomeSetupChecklistEvidence.builder()
                .coupleConnected(true)
                .accountLinked(true)
                .weddingGoalCreated(true)
                .regionSelected(true)
                .firstSavingRecorded(true)
                .build());

        Assertions.assertEquals(5, result.getCompletedCount());
        Assertions.assertNull(result.getCurrentStep());
        Assertions.assertTrue(result.getSteps().stream().allMatch(stepResult -> stepResult.getCompleted()));
        Assertions.assertTrue(result.getSteps().stream()
                .allMatch(stepResult -> Status.COMPLETED.equals(stepResult.getStatus())));
    }
}
