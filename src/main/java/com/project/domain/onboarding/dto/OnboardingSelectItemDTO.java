package com.project.domain.onboarding.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "온보딩 선택 계좌/카드 정보 DTO")
public class OnboardingSelectItemDTO {

    @NotBlank(message = "선택한 계좌/카드 번호는 필수입니다.")
    @ApiModelProperty(value = "계좌 또는 카드 번호", required = true, example = "12345678901234")
    private String number;

    @ApiModelProperty(value = "계좌 또는 카드 상품명/별칭 (예: KB국민ONE통장-저축예금)", example = "KB국민ONE통장-저축예금")
    private String name;
}
