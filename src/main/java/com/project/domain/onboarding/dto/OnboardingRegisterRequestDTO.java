package com.project.domain.onboarding.dto;

import com.project.domain.codef.dto.CodefAccountItemDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "온보딩 계좌/카드 등록 및 추가 요청 DTO")
public class OnboardingRegisterRequestDTO {

    @NotNull(message = "금융기관 계정 정보는 필수입니다.")
    @Valid
    @ApiModelProperty(value = "등록 또는 추가할 금융기관 계정 정보", required = true)
    private CodefAccountItemDTO accountItem;
}
