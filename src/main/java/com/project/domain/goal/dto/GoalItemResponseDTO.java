package com.project.domain.goal.dto;

import com.project.domain.goal.domain.GoalItemVO;
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
@ApiModel(description = "결혼 목표 항목 조회 응답")
public class GoalItemResponseDTO {

    @ApiModelProperty(value = "목표 항목 ID", example = "1")
    private Long goalItemId;

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

    public static GoalItemResponseDTO from(GoalItemVO item) {
        return GoalItemResponseDTO.builder()
                .goalItemId(item.getId())
                .category(item.getCategory())
                .targetAmount(item.getTargetAmount())
                .included(item.getIncluded())
                .completed(item.getCompleted())
                .type(item.getType())
                .build();
    }
}
