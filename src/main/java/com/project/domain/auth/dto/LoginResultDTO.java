package com.project.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResultDTO {

    private LoginResponseDTO response;
    private String refreshToken;
}
