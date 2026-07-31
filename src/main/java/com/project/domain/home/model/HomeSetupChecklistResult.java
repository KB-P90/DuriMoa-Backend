package com.project.domain.home.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HomeSetupChecklistResult {

    // 체크리스트 정의에 포함된 전체 단계 수입니다.
    private final Integer totalCount;

    // 앞 단계부터 연속해서 완료된 유효 완료 단계 수입니다.
    private final Integer completedCount;

    // 처음으로 완료되지 않은 단계 번호이며 전체 완료 시 null입니다.
    private final Integer currentStep;

    // 단계별 유효 완료 여부와 화면 상태입니다.
    private final List<StepResult> steps;

    @Getter
    @Builder
    public static class StepResult {

        // 체크리스트의 고정 단계 정의입니다.
        private final Step step;

        // 앞 단계 조건까지 만족해 완료로 인정되는지 나타냅니다.
        private final Boolean completed;

        // 화면에서 사용할 단계 상태입니다.
        private final Status status;
    }

    public enum Step {

        // 실제 활성 커플 연결이 완료된 상태를 확인하는 단계입니다.
        COUPLE_CONNECTED(1, "COUPLE_CONNECTED", "커플 연결"),

        // 인증 사용자에게 실제 연결 계좌 또는 카드가 있는지 확인하는 단계입니다.
        ACCOUNT_LINKED(2, "ACCOUNT_LINKED", "계좌 연결"),

        // 활성 커플에 대표 결혼 목표가 있는지 확인하는 단계입니다.
        WEDDING_GOAL_CREATED(3, "WEDDING_GOAL_CREATED", "결혼 목표 설정"),

        // 대표 결혼 목표에 지역 값이 저장되어 있는지 확인하는 단계입니다.
        REGION_SELECTED(4, "REGION_SELECTED", "지역 선택"),

        // 활성 커플 구성원의 실제 양수 저축 기록이 있는지 확인하는 단계입니다.
        FIRST_SAVING_RECORDED(5, "FIRST_SAVING_RECORDED", "첫 저축 등록");

        // 화면에 표시할 고정 단계 번호입니다.
        private final Integer step;

        // 프론트가 단계 의미를 식별할 때 사용할 고정 코드입니다.
        private final String code;

        // 체크리스트에 표시할 단계 제목입니다.
        private final String title;

        // 단계 정의의 고정 값을 초기화합니다.
        Step(Integer step, String code, String title) {
            this.step = step;
            this.code = code;
            this.title = title;
        }

        // 단계 번호를 반환합니다.
        public Integer getStep() {
            return step;
        }

        // 단계 코드를 반환합니다.
        public String getCode() {
            return code;
        }

        // 단계 제목을 반환합니다.
        public String getTitle() {
            return title;
        }
    }

    public enum Status {

        // 앞 단계 조건까지 모두 만족해 완료된 상태입니다.
        COMPLETED,

        // 사용자가 지금 수행해야 하는 첫 미완료 단계입니다.
        CURRENT,

        // 앞 단계가 끝나지 않아 아직 열리지 않은 단계입니다.
        LOCKED
    }
}
