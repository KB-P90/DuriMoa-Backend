package com.project.domain.home.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SetupChecklistStepResponseDTO {

    // 체크리스트에서 보여줄 단계 번호입니다.
    @JsonProperty("step")
    private final Integer step;

    // 프론트가 단계 의미를 구분할 때 사용할 고정 코드입니다.
    @JsonProperty("code")
    private final String code;

    // 화면에 표시할 단계 제목입니다.
    @JsonProperty("title")
    private final String title;

    // 앞 단계 조건까지 만족해 실제 완료로 인정된 여부입니다.
    @JsonProperty("completed")
    private final Boolean completed;

    // COMPLETED, CURRENT, LOCKED 중 하나인 화면 상태입니다.
    @JsonProperty("status")
    private final String status;
}
