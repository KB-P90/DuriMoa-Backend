package com.project.domain.monthly.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "절약 미션 정보 DTO")
public class SavingMissionDTO {

    @ApiModelProperty(value = "절약 미션 ID", example = "101")
    private Long missionId;

    @ApiModelProperty(value = "카테고리 코드", example = "FOOD")
    private String categoryCode;

    @ApiModelProperty(value = "미션 제목", example = "주 3회에서 주 1회로 줄이기")
    private String title;

    @ApiModelProperty(value = "예상 절감 금액", example = "30000")
    private Long expectedSavingAmount;

    @JsonProperty("isSelectable")
    @ApiModelProperty(value = "미션 선택/도전 가능 여부 (도전중, 완료: false, 도전하기: true)", example = "true")
    private Boolean isSelectable;

    @ApiModelProperty(value = "미션 상태 (도전하기, 도전중, 완료)", example = "도전하기")
    private String status;
}
