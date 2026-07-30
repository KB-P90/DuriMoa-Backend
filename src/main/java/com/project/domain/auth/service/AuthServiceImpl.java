package com.project.domain.auth.service;

import com.project.common.security.JwtTokenProvider;
import com.project.domain.auth.dto.LoginRequestDTO;
import com.project.domain.auth.dto.LoginResponseDTO;
import com.project.domain.auth.dto.LoginUserDTO;
import com.project.domain.auth.dto.LoginUserResponseDTO;
import com.project.domain.auth.dto.SignupRequestDTO;
import com.project.domain.auth.dto.SignupResponseDTO;
import com.project.domain.auth.exception.InvalidLoginCredentialsException;
import com.project.domain.auth.exception.PasswordConfirmationMismatchException;
import com.project.domain.auth.exception.PhoneDuplicateException;
import com.project.domain.auth.mapper.AuthMapper;
import com.project.domain.couple.service.CoupleInvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CoupleInvitationService coupleInvitationService;

    @Override
    @Transactional
    public SignupResponseDTO signup(SignupRequestDTO request) {
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new PasswordConfirmationMismatchException();
        }

        String normalizedPhone = request.getPhone().replace("-", "");
        if (Boolean.TRUE.equals(authMapper.existsUserByPhone(normalizedPhone))) {
            throw new PhoneDuplicateException();
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        try {
            authMapper.insertUser(
                    normalizedPhone,
                    encodedPassword,
                    request.getName(),
                    request.getRole());
        } catch (DuplicateKeyException exception) {
            throw new PhoneDuplicateException();
        }

        SignupResponseDTO response = authMapper.findSignedUpUserByPhone(normalizedPhone);
        coupleInvitationService.createInviteCode(response.getUserId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO request) {
        String normalizedPhone = request.getPhone().replace("-", "");
        LoginUserDTO loginUser = authMapper.findLoginUserByPhone(normalizedPhone);

        if (loginUser == null
                || loginUser.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), loginUser.getPassword())) {
            throw new InvalidLoginCredentialsException();
        }

        String accessToken =
                jwtTokenProvider.createAccessToken(loginUser.getUserId(), loginUser.getRole());
        LoginUserResponseDTO user = new LoginUserResponseDTO(
                loginUser.getUserId(),
                loginUser.getPhone(),
                loginUser.getName(),
                loginUser.getRole(),
                loginUser.getProfileImage(),
                loginUser.getProvider());
        return new LoginResponseDTO(accessToken, user);
    }
}
