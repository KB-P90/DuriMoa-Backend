package com.project.domain.goal.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "결혼 목표 항목 등록 요청")
public class GoalItemCreateRequestDTO {

    @NotBlank(message = "목표 항목 카테고리는 필수입니다.")
    @Size(max = 30, message = "목표 항목 카테고리는 30자 이하여야 합니다.")
    @ApiModelProperty(value = "목표 항목 카테고리", example = "예식장", required = true)
    private String category;

    @NotNull(message = "목표 금액은 필수입니다.")
    @PositiveOrZero(message = "목표 금액은 0 이상이어야 합니다.")
    @ApiModelProperty(value = "목표 금액", example = "18000000", required = true)
    private Long targetAmount;

    @NotNull(message = "예산 포함 여부는 필수입니다.")
    @ApiModelProperty(value = "총예산 포함 여부", example = "true", required = true)
    private Boolean included;

    @NotBlank(message = "예산 유형은 필수입니다.")
    @Size(max = 30, message = "예산 유형은 30자 이하여야 합니다.")
    @ApiModelProperty(value = "예산 유형", allowableValues = "알뜰형,균형형,플렉스형",
            example = "균형형", required = true)
    private String type;
}
