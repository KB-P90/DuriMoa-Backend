package com.project.domain.auth.service;

import com.project.common.security.JwtTokenProvider;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.auth.dto.LoginRequestDTO;
import com.project.domain.auth.dto.LoginResponseDTO;
import com.project.domain.auth.dto.LoginResultDTO;
import com.project.domain.auth.dto.LoginUserDTO;
import com.project.domain.auth.dto.LoginUserResponseDTO;
import com.project.domain.auth.client.KakaoAuthClient;
import com.project.domain.auth.dto.KakaoLoginRequestDTO;
import com.project.domain.auth.dto.KakaoLoginResponseDTO;
import com.project.domain.auth.dto.KakaoLoginResultDTO;
import com.project.domain.auth.dto.KakaoSignupRequestDTO;
import com.project.domain.auth.dto.KakaoUserInfoDTO;
import com.project.domain.auth.dto.SignupRequestDTO;
import com.project.domain.auth.dto.SignupResponseDTO;
import com.project.domain.auth.dto.TokenRefreshResponseDTO;
import com.project.domain.auth.dto.TokenRefreshResultDTO;
import com.project.domain.auth.mapper.AuthMapper;
import com.project.domain.auth.repository.RefreshTokenRepository;
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
    private static final String FINANCE_TERMS = "FINANCE";
    private static final String KAKAO_PROVIDER = "KAKAO";

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CoupleInvitationService coupleInvitationService;
    private final KakaoAuthClient kakaoAuthClient;

    @Override
    @Transactional
    public SignupResponseDTO signup(SignupRequestDTO request) {
        validateLocalSignupRequest(request);
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }
        validateRequiredTerms(
                request.getServiceTermsAgreed(),
                request.getPrivacyTermsAgreed(),
                request.getFinanceTermsAgreed());

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
    @Transactional
    public LoginResultDTO login(LoginRequestDTO request) {
        String normalizedPhone = request.getPhone().replace("-", "");
        LoginUserDTO loginUser = authMapper.findLoginUserByPhone(normalizedPhone);

        if (loginUser == null
                || loginUser.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), loginUser.getPassword())) {
            throw new CustomException(
                    ErrorCode.UNAUTHORIZED,
                    "휴대폰 번호 또는 비밀번호가 일치하지 않습니다.");
        }

        return issueLoginTokens(loginUser);
    }

    @Override
    @Transactional
    public KakaoLoginResultDTO loginWithKakao(KakaoLoginRequestDTO request) {
        KakaoUserInfoDTO kakaoUser = kakaoAuthClient.getUserInfo(request.getCode());
        String providerId = String.valueOf(kakaoUser.getId());
        LoginUserDTO loginUser = authMapper.findLoginUserByProviderId(
                KAKAO_PROVIDER,
                providerId);

        if (loginUser != null) {
            LoginResultDTO loginResult = issueLoginTokens(loginUser);
            return new KakaoLoginResultDTO(
                    KakaoLoginResponseDTO.loggedIn(
                            loginResult.getResponse().getAccessToken(),
                            loginResult.getResponse().getUser()),
                    loginResult.getRefreshToken());
        }

        validateKakaoProfile(kakaoUser);
        String signupToken = jwtTokenProvider.createKakaoSignupToken(
                providerId,
                kakaoUser.getName(),
                kakaoUser.getProfileImage());
        return new KakaoLoginResultDTO(
                KakaoLoginResponseDTO.signupRequired(
                        signupToken,
                        kakaoUser.getName(),
                        kakaoUser.getProfileImage()),
                null);
    }

    @Override
    @Transactional
    public LoginResultDTO signupWithKakao(KakaoSignupRequestDTO request) {
        validateRequiredTerms(
                request.getServiceTermsAgreed(),
                request.getPrivacyTermsAgreed(),
                request.getFinanceTermsAgreed());
        if (!jwtTokenProvider.validateKakaoSignupToken(request.getSignupToken())) {
            throw new CustomException(
                    ErrorCode.UNAUTHORIZED,
                    "유효하지 않거나 만료된 카카오 가입 토큰입니다.");
        }

        String providerId = jwtTokenProvider.getKakaoProviderId(request.getSignupToken());
        String name = jwtTokenProvider.getKakaoName(request.getSignupToken());
        String profileImage = jwtTokenProvider.getKakaoProfileImage(request.getSignupToken());
        if (providerId == null || providerId.isBlank() || name == null || name.isBlank()) {
            throw new CustomException(
                    ErrorCode.UNAUTHORIZED,
                    "유효하지 않은 카카오 가입 정보입니다.");
        }
        if (authMapper.findLoginUserByProviderId(KAKAO_PROVIDER, providerId) != null) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "이미 가입된 카카오 계정입니다.");
        }

        try {
            authMapper.insertKakaoUser(
                    providerId,
                    name,
                    request.getRole(),
                    profileImage);
        } catch (DuplicateKeyException exception) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "이미 가입된 카카오 계정입니다.");
        }

        LoginUserDTO loginUser = authMapper.findLoginUserByProviderId(
                KAKAO_PROVIDER,
                providerId);
        if (loginUser == null) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        saveTermsAgreements(
                loginUser.getUserId(),
                request.getServiceTermsAgreed(),
                request.getPrivacyTermsAgreed(),
                request.getFinanceTermsAgreed());
        coupleInvitationService.createInviteCode(loginUser.getUserId());
        return issueLoginTokens(loginUser);
    }

    @Override
    @Transactional
    public TokenRefreshResultDTO refresh(String refreshToken) {
        if (refreshToken == null
                || refreshToken.isBlank()
                || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw invalidRefreshTokenException();
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        LoginUserDTO loginUser = authMapper.findLoginUserById(userId);
        if (loginUser == null) {
            refreshTokenRepository.delete(userId);
            throw invalidRefreshTokenException();
        }

        String newAccessToken =
                jwtTokenProvider.createAccessToken(loginUser.getUserId(), loginUser.getRole());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(loginUser.getUserId());
        Boolean rotated = refreshTokenRepository.rotate(
                loginUser.getUserId(),
                refreshToken,
                newRefreshToken,
                jwtTokenProvider.getRefreshTokenExpiration());
        if (!Boolean.TRUE.equals(rotated)) {
            throw invalidRefreshTokenException();
        }

        TokenRefreshResponseDTO response =
                new TokenRefreshResponseDTO("Bearer " + newAccessToken);
        return new TokenRefreshResultDTO(response, newRefreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken == null
                || refreshToken.isBlank()
                || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
            return;
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        refreshTokenRepository.deleteIfMatches(userId, refreshToken);
    }

    private void saveTermsAgreements(Long userId, SignupRequestDTO request) {
        saveTermsAgreements(
                userId,
                request.getServiceTermsAgreed(),
                request.getPrivacyTermsAgreed(),
                request.getFinanceTermsAgreed());
    }

    private void saveTermsAgreements(
            Long userId,
            Boolean serviceTermsAgreed,
            Boolean privacyTermsAgreed,
            Boolean financeTermsAgreed) {
        authMapper.insertTermsAgreement(userId, SERVICE_TERMS, serviceTermsAgreed);
        authMapper.insertTermsAgreement(userId, PRIVACY_TERMS, privacyTermsAgreed);
        authMapper.insertTermsAgreement(userId, FINANCE_TERMS, financeTermsAgreed);
    }

    private LoginResultDTO issueLoginTokens(LoginUserDTO loginUser) {
        String accessToken =
                jwtTokenProvider.createAccessToken(loginUser.getUserId(), loginUser.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(loginUser.getUserId());
        refreshTokenRepository.save(
                loginUser.getUserId(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpiration());
        LoginUserResponseDTO user = new LoginUserResponseDTO(
                loginUser.getUserId(),
                loginUser.getPhone(),
                loginUser.getName(),
                loginUser.getRole(),
                loginUser.getProfileImage(),
                loginUser.getProvider());
        return new LoginResultDTO(
                new LoginResponseDTO("Bearer " + accessToken, user),
                refreshToken);
    }

    private void validateLocalSignupRequest(SignupRequestDTO request) {
        if (request.getPhone() == null
                || request.getPhone().isBlank()
                || request.getPassword() == null
                || request.getPassword().isBlank()) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "일반 회원가입에는 휴대폰 번호와 비밀번호가 필요합니다.");
        }
    }

    private void validateRequiredTerms(
            Boolean serviceTermsAgreed,
            Boolean privacyTermsAgreed,
            Boolean financeTermsAgreed) {
        if (!Boolean.TRUE.equals(serviceTermsAgreed)
                || !Boolean.TRUE.equals(privacyTermsAgreed)
                || !Boolean.TRUE.equals(financeTermsAgreed)) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "필수 약관에 모두 동의해야 회원가입이 가능합니다.");
        }
    }

    private void validateKakaoProfile(KakaoUserInfoDTO kakaoUser) {
        if (kakaoUser.getName() == null
                || kakaoUser.getName().isBlank()
                || kakaoUser.getProfileImage() == null
                || kakaoUser.getProfileImage().isBlank()) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "카카오 이름과 프로필 사진 제공에 동의해야 합니다.");
        }
    }

    private CustomException invalidRefreshTokenException() {
        return new CustomException(
                ErrorCode.UNAUTHORIZED,
                "유효하지 않거나 만료된 Refresh Token입니다.");
    }
}
