package com.project.domain.auth.service;

import com.project.domain.auth.dto.LoginRequestDTO;
import com.project.domain.auth.dto.LoginResultDTO;
import com.project.domain.auth.dto.KakaoLoginRequestDTO;
import com.project.domain.auth.dto.KakaoLoginResultDTO;
import com.project.domain.auth.dto.KakaoSignupRequestDTO;
import com.project.domain.auth.dto.SignupRequestDTO;
import com.project.domain.auth.dto.SignupResponseDTO;
import com.project.domain.auth.dto.TokenRefreshResultDTO;

public interface AuthService {

    SignupResponseDTO signup(SignupRequestDTO request);

    LoginResultDTO login(LoginRequestDTO request);

    KakaoLoginResultDTO loginWithKakao(KakaoLoginRequestDTO request);

    LoginResultDTO signupWithKakao(KakaoSignupRequestDTO request);

    TokenRefreshResultDTO refresh(String refreshToken);

    void logout(String refreshToken);
}
