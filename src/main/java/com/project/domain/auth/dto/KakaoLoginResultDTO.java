package com.project.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KakaoLoginResultDTO {

    private KakaoLoginResponseDTO response;
    private String refreshToken;
}
