package com.project.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Getter
@NoArgsConstructor
public class KakaoLoginRequestDTO {

    @NotBlank(message = "카카오 인가 코드는 필수입니다.")
    private String code;
}
