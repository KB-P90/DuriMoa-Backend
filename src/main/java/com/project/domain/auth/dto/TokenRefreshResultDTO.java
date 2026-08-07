package com.project.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenRefreshResultDTO {

    private TokenRefreshResponseDTO response;
    private String refreshToken;
}
