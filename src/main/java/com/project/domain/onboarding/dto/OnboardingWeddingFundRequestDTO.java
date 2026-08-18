package com.project.domain.onboarding.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "온보딩 결혼 자금 입력 요청 DTO")
public class OnboardingWeddingFundRequestDTO {

    @NotBlank(message = "결혼 자금(weddingFund)은 필수입니다.")
    @Pattern(regexp = "^[0-9]+$", message = "결혼 자금(weddingFund)은 0 이상의 숫자만 입력할 수 있습니다.")
    @ApiModelProperty(value = "결혼 자금으로 포함할 현재 보유 금액", required = true, example = "2000000")
    private String weddingFund;
}
