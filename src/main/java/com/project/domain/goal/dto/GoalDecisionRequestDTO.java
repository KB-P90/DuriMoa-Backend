package com.project.domain.goal.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "메인 시안 승인 요청 응답")
public class GoalDecisionRequestDTO {

    @NotBlank(message = "승인 여부는 필수입니다.")
    @ApiModelProperty(value = "승인 여부", allowableValues = "APPROVE,REJECT", example = "APPROVE")
    private String decision;
}
