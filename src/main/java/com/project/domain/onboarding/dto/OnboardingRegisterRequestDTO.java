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
@ApiModel(description = "온보딩 계좌/카드 등록 및 추가 요청 DTO")
public class OnboardingRegisterRequestDTO {

    @NotBlank(message = "금융기관 이름(company)은 필수입니다.")
    @ApiModelProperty(value = "금융기관 이름 또는 코드 (예: KB국민은행, KB국민카드)", required = true, example = "KB국민은행")
    private String company;

    @NotBlank(message = "아이디(id)는 필수입니다.")
    @ApiModelProperty(value = "사용자 금융기관 로그인 아이디", required = true, example = "user1234")
    private String id;

    @NotBlank(message = "비밀번호(password)는 필수입니다.")
    @ApiModelProperty(value = "사용자 금융기관 로그인 비밀번호", required = true, example = "password123!")
    private String password;
}
