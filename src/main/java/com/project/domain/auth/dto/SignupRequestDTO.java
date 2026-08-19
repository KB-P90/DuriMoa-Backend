package com.project.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@NoArgsConstructor
public class SignupRequestDTO {

    @NotBlank(message = "휴대폰 번호는 필수입니다.")
    @Pattern(
            regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$",
            message = "올바른 휴대폰 번호 형식이 아닙니다.")
    private String phone;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수입니다.")
    @Size(min = 8, max = 72, message = "비밀번호 확인은 8자 이상 72자 이하여야 합니다.")
    private String passwordConfirm;

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "역할은 필수입니다.")
    @Pattern(regexp = "^[BG]$", message = "역할은 B 또는 G여야 합니다.")
    private String role;

    @NotNull(message = "서비스 이용약관 동의는 필수입니다.")
    @AssertTrue(message = "서비스 이용약관에 동의해야 합니다.")
    private Boolean serviceTermsAgreed;

    @NotNull(message = "개인정보 수집 및 이용 동의는 필수입니다.")
    @AssertTrue(message = "개인정보 수집 및 이용에 동의해야 합니다.")
    private Boolean privacyTermsAgreed;

    @NotNull(message = "금융정보 연동 약관 동의는 필수입니다.")
    @AssertTrue(message = "금융정보 연동 약관에 동의해야 합니다.")
    private Boolean financeTermsAgreed;
}
