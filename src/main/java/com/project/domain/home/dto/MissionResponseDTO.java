package com.project.domain.home.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MissionResponseDTO {

    // 대표 미션 식별자입니다.
    @JsonProperty("mission_id")
    private final Long missionId;

    // 대표 미션 카테고리 코드입니다.
    @JsonProperty("category_code")
    private final String categoryCode;

    // 대표 미션 제목입니다.
    @JsonProperty("title")
    private final String title;

    // 대표 미션 상태입니다.
    @JsonProperty("status")
    private final String status;

    // 대표 미션의 예상 절약 금액입니다.
    @JsonProperty("expected_saving_amount")
    private final Long expectedSavingAmount;
}
