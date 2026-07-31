package com.project.domain.auth.service;

import com.project.common.security.JwtTokenProvider;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.auth.dto.LoginRequestDTO;
import com.project.domain.auth.dto.LoginResponseDTO;
import com.project.domain.auth.dto.LoginUserDTO;
import com.project.domain.auth.dto.LoginUserResponseDTO;
import com.project.domain.auth.dto.SignupRequestDTO;
import com.project.domain.auth.dto.SignupResponseDTO;
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

    private static final String SERVICE_TERMS = "SERVICE";
    private static final String PRIVACY_TERMS = "PRIVACY";
    private static final String MARKETING_TERMS = "MARKETING";

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CoupleInvitationService coupleInvitationService;

    @Override
    @Transactional
    public SignupResponseDTO signup(SignupRequestDTO request) {
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }
        if (!Boolean.TRUE.equals(request.getServiceTermsAgreed())
                || !Boolean.TRUE.equals(request.getPrivacyTermsAgreed())) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "필수 약관에 동의해야 회원가입이 가능합니다.");
        }

        String normalizedPhone = request.getPhone().replace("-", "");
        if (Boolean.TRUE.equals(authMapper.existsUserByPhone(normalizedPhone))) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "이미 가입된 휴대폰 번호입니다.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        try {
            authMapper.insertUser(
                    normalizedPhone,
                    encodedPassword,
                    request.getName(),
                    request.getRole());
        } catch (DuplicateKeyException exception) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "이미 가입된 휴대폰 번호입니다.");
        }

        SignupResponseDTO response = authMapper.findSignedUpUserByPhone(normalizedPhone);
        saveTermsAgreements(response.getUserId(), request);
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
            throw new CustomException(
                    ErrorCode.UNAUTHORIZED,
                    "휴대폰 번호 또는 비밀번호가 일치하지 않습니다.");
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
        return new LoginResponseDTO("Bearer " + accessToken, user);
    }

    private void saveTermsAgreements(Long userId, SignupRequestDTO request) {
        authMapper.insertTermsAgreement(userId, SERVICE_TERMS, request.getServiceTermsAgreed());
        authMapper.insertTermsAgreement(userId, PRIVACY_TERMS, request.getPrivacyTermsAgreed());
        authMapper.insertTermsAgreement(userId, MARKETING_TERMS, request.getMarketingTermsAgreed());
    }
}
