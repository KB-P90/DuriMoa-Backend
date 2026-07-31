package com.project.domain.codef.dto;

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
@ApiModel(description = "CODEF 금융기관 계정 정보")
public class CodefAccountItemDTO {

    @NotBlank(message = "국가 코드는 필수입니다.")
    @ApiModelProperty(value = "국가 코드 (예: KR)", required = true, example = "KR")
    @Builder.Default
    private String countryCode = "KR";

    @NotBlank(message = "업무 구분은 필수입니다.")
    @ApiModelProperty(value = "업무 구분 (BK: 은행, CD: 카드, WM: 증권 등)", required = true, example = "BK")
    private String businessType;

    @NotBlank(message = "고객 구분은 필수입니다.")
    @ApiModelProperty(value = "고객 구분 (P: 개인, B: 기업)", required = true, example = "P")
    private String clientType;

    @NotBlank(message = "기관 코드는 필수입니다.")
    @ApiModelProperty(value = "기관 코드 (예: 0004 KB국민은행, 0088 신한은행, 0301 KB국민카드)", required = true, example = "0004")
    private String organization;

    @NotBlank(message = "로그인 방식은 필수입니다.")
    @ApiModelProperty(value = "로그인 방식 (1: 아이디/패스워드)", required = true, example = "1")
    private String loginType;

    @NotBlank(message = "아이디는 필수입니다.")
    @ApiModelProperty(value = "금융기관 로그인 아이디", required = true, example = "user_id_example")
    private String id;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @ApiModelProperty(value = "금융기관 로그인 비밀번호 (평문 입력 시 백엔드에서 자동 RSA 암호화)", required = true, example = "password123!")
    private String password;
}
