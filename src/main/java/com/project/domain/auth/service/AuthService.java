package com.project.domain.auth.service;

import com.project.domain.auth.dto.LoginRequestDTO;
import com.project.domain.auth.dto.LoginResponseDTO;
import com.project.domain.auth.dto.SignupRequestDTO;
import com.project.domain.auth.dto.SignupResponseDTO;

public interface AuthService {

    SignupResponseDTO signup(SignupRequestDTO request);

    LoginResponseDTO login(LoginRequestDTO request);
}
