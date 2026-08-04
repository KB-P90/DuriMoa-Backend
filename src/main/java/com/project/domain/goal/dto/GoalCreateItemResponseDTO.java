package com.project.domain.goal.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "결혼 시안 등록 항목 응답")
public class GoalCreateItemResponseDTO {

    @ApiModelProperty(value = "목표 항목 카테고리", example = "예식장")
    private String category;

    @ApiModelProperty(value = "목표 금액", example = "18000000")
    private Long targetAmount;

    @ApiModelProperty(value = "총예산 포함 여부", example = "true")
    private Boolean included;

    @ApiModelProperty(value = "완료 여부", example = "false")
    private Boolean completed;

    @ApiModelProperty(value = "예산 유형", example = "균형형")
    private String type;

    public static GoalCreateItemResponseDTO from(GoalItemCreateRequestDTO item) {
        return GoalCreateItemResponseDTO.builder()
                .category(item.getCategory().trim())
                .targetAmount(item.getTargetAmount())
                .included(item.getIncluded())
                .completed(false)
                .type(item.getType().trim())
                .build();
    }
}
