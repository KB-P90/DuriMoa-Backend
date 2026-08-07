package com.project.domain.auth.service;

import com.project.domain.auth.dto.LoginRequestDTO;
import com.project.domain.auth.dto.LoginResultDTO;
import com.project.domain.auth.dto.SignupRequestDTO;
import com.project.domain.auth.dto.SignupResponseDTO;
import com.project.domain.auth.dto.TokenRefreshResultDTO;

public interface AuthService {

    SignupResponseDTO signup(SignupRequestDTO request);

    LoginResultDTO login(LoginRequestDTO request);

    TokenRefreshResultDTO refresh(String refreshToken);

    void logout(String refreshToken);
}
