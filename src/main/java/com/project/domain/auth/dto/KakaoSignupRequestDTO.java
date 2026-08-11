package com.project.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Getter
@NoArgsConstructor
public class KakaoSignupRequestDTO {

    @NotBlank(message = "카카오 가입 토큰은 필수입니다.")
    private String signupToken;

    @NotBlank(message = "역할은 필수입니다.")
    @Pattern(regexp = "^[BG]$", message = "역할은 B 또는 G여야 합니다.")
    private String role;

    @NotNull(message = "서비스 이용약관 동의는 필수입니다.")
    private Boolean serviceTermsAgreed;

    @NotNull(message = "개인정보 수집 및 이용 동의는 필수입니다.")
    private Boolean privacyTermsAgreed;

    @NotNull(message = "마케팅 정보 수신 동의 여부는 필수입니다.")
    private Boolean marketingTermsAgreed;
}
