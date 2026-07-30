package com.project.domain.couple.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InviteCodeRequestDTO {

    @NotBlank(message = "초대코드는 필수입니다.")
    @Pattern(regexp = "^[A-Z0-9]{6}$", message = "초대코드는 영문 대문자와 숫자로 이루어진 6자리여야 합니다.")
    private String inviteCode;
}
