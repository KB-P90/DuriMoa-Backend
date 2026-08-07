package com.project.domain.auth.service;

import com.project.common.security.JwtTokenProvider;
import com.project.common.exception.CustomException;
import com.project.domain.auth.dto.LoginRequestDTO;
import com.project.domain.auth.dto.LoginResultDTO;
import com.project.domain.auth.dto.LoginUserDTO;
import com.project.domain.auth.dto.SignupRequestDTO;
import com.project.domain.auth.dto.SignupResponseDTO;
import com.project.domain.auth.dto.TokenRefreshResultDTO;
import com.project.domain.auth.mapper.AuthMapper;
import com.project.domain.auth.repository.RefreshTokenRepository;
import com.project.domain.couple.service.CoupleInvitationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthMapper authMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private CoupleInvitationService coupleInvitationService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                authMapper,
                passwordEncoder,
                jwtTokenProvider,
                refreshTokenRepository,
                coupleInvitationService);
    }

    @Test
    void signupEncodesPasswordAndNormalizesPhone() throws Exception {
        SignupRequestDTO request =
                createRequest("010-1234-5678", "password123", "password123", "홍길동", "G");
        SignupResponseDTO expectedResponse = new SignupResponseDTO(
                1L, "01012345678", "홍길동", "G", "LOCAL", null, LocalDateTime.now());
        when(authMapper.existsUserByPhone("01012345678")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(authMapper.findSignedUpUserByPhone("01012345678")).thenReturn(expectedResponse);

        SignupResponseDTO response = authService.signup(request);

        verify(authMapper).insertUser(
                "01012345678", "encodedPassword", "홍길동", "G");
        verify(authMapper).insertTermsAgreement(1L, "SERVICE", true);
        verify(authMapper).insertTermsAgreement(1L, "PRIVACY", true);
        verify(authMapper).insertTermsAgreement(1L, "MARKETING", false);
        assertEquals(1L, response.getUserId());
        assertEquals("01012345678", response.getPhone());
        assertEquals("LOCAL", response.getProvider());
    }

    @Test
    void signupRejectsDuplicatePhone() throws Exception {
        SignupRequestDTO request =
                createRequest("01012345678", "password123", "password123", "홍길동", "G");
        when(authMapper.existsUserByPhone("01012345678")).thenReturn(true);

        assertThrows(CustomException.class, () -> authService.signup(request));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void signupRejectsPasswordConfirmationMismatch() throws Exception {
        SignupRequestDTO request =
                createRequest("01012345678", "password123", "different123", "홍길동", "G");

        assertThrows(
                CustomException.class,
                () -> authService.signup(request));
        verify(authMapper, never()).existsUserByPhone(anyString());
    }

    @Test
    void signupRejectsRequiredTermsAgreementMissing() throws Exception {
        SignupRequestDTO request = createRequest(
                "01012345678",
                "password123",
                "password123",
                "홍길동",
                "G",
                false,
                true,
                false);

        assertThrows(CustomException.class, () -> authService.signup(request));
        verify(authMapper, never()).existsUserByPhone(anyString());
    }

    @Test
    void loginReturnsAccessTokenAndUserWhenCredentialsAreValid() throws Exception {
        LoginRequestDTO request = createLoginRequest("010-1234-5678", "password123");
        LoginUserDTO loginUser = new LoginUserDTO(
                1L,
                "01012345678",
                "encodedPassword",
                "홍길동",
                "G",
                null,
                "LOCAL");
        when(authMapper.findLoginUserByPhone("01012345678")).thenReturn(loginUser);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(1L, "G")).thenReturn("accessToken");
        when(jwtTokenProvider.createRefreshToken(1L)).thenReturn("refreshToken");
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(Duration.ofDays(14));

        LoginResultDTO result = authService.login(request);

        assertEquals("Bearer accessToken", result.getResponse().getAccessToken());
        assertEquals(1L, result.getResponse().getUser().getUserId());
        assertEquals("01012345678", result.getResponse().getUser().getPhone());
        assertEquals("LOCAL", result.getResponse().getUser().getProvider());
        assertEquals("refreshToken", result.getRefreshToken());
        verify(refreshTokenRepository).save(1L, "refreshToken", Duration.ofDays(14));
    }

    @Test
    void loginRejectsUnknownPhone() throws Exception {
        LoginRequestDTO request = createLoginRequest("01012345678", "password123");
        when(authMapper.findLoginUserByPhone("01012345678")).thenReturn(null);

        assertThrows(CustomException.class, () -> authService.login(request));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void loginRejectsIncorrectPassword() throws Exception {
        LoginRequestDTO request = createLoginRequest("01012345678", "wrongPassword");
        LoginUserDTO loginUser = new LoginUserDTO(
                1L,
                "01012345678",
                "encodedPassword",
                "홍길동",
                "G",
                null,
                "LOCAL");
        when(authMapper.findLoginUserByPhone("01012345678")).thenReturn(loginUser);
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThrows(CustomException.class, () -> authService.login(request));
        verify(jwtTokenProvider, never()).createAccessToken(1L, "G");
    }

    @Test
    void refreshRotatesTokenAndReturnsNewAccessToken() {
        LoginUserDTO loginUser = new LoginUserDTO(
                1L, null, null, null, "G", null, null);
        when(jwtTokenProvider.validateRefreshToken("refreshToken")).thenReturn(true);
        when(jwtTokenProvider.getUserId("refreshToken")).thenReturn(1L);
        when(authMapper.findLoginUserById(1L)).thenReturn(loginUser);
        when(jwtTokenProvider.createAccessToken(1L, "G")).thenReturn("newAccessToken");
        when(jwtTokenProvider.createRefreshToken(1L)).thenReturn("newRefreshToken");
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(Duration.ofDays(14));
        when(refreshTokenRepository.rotate(
                1L,
                "refreshToken",
                "newRefreshToken",
                Duration.ofDays(14)))
                .thenReturn(true);

        TokenRefreshResultDTO result = authService.refresh("refreshToken");

        assertEquals("Bearer newAccessToken", result.getResponse().getAccessToken());
        assertEquals("newRefreshToken", result.getRefreshToken());
    }

    @Test
    void refreshRejectsInvalidTokenBeforeDatabaseLookup() {
        when(jwtTokenProvider.validateRefreshToken("invalidToken")).thenReturn(false);

        assertThrows(CustomException.class, () -> authService.refresh("invalidToken"));

        verify(authMapper, never()).findLoginUserById(1L);
    }

    @Test
    void refreshRejectsReusedToken() {
        LoginUserDTO loginUser = new LoginUserDTO(
                1L, null, null, null, "G", null, null);
        when(jwtTokenProvider.validateRefreshToken("reusedToken")).thenReturn(true);
        when(jwtTokenProvider.getUserId("reusedToken")).thenReturn(1L);
        when(authMapper.findLoginUserById(1L)).thenReturn(loginUser);
        when(jwtTokenProvider.createAccessToken(1L, "G")).thenReturn("newAccessToken");
        when(jwtTokenProvider.createRefreshToken(1L)).thenReturn("newRefreshToken");
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(Duration.ofDays(14));
        when(refreshTokenRepository.rotate(
                1L,
                "reusedToken",
                "newRefreshToken",
                Duration.ofDays(14)))
                .thenReturn(false);

        assertThrows(CustomException.class, () -> authService.refresh("reusedToken"));
    }

    @Test
    void logoutDeletesMatchingRefreshToken() {
        when(jwtTokenProvider.validateRefreshToken("refreshToken")).thenReturn(true);
        when(jwtTokenProvider.getUserId("refreshToken")).thenReturn(1L);

        authService.logout("refreshToken");

        verify(refreshTokenRepository).deleteIfMatches(1L, "refreshToken");
    }

    @Test
    void logoutIsIdempotentWhenRefreshTokenIsMissing() {
        authService.logout(null);

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void logoutDoesNotDeleteSessionForInvalidRefreshToken() {
        when(jwtTokenProvider.validateRefreshToken("invalidToken")).thenReturn(false);

        authService.logout("invalidToken");

        verifyNoInteractions(refreshTokenRepository);
    }

    private SignupRequestDTO createRequest(
            String phone,
            String password,
            String passwordConfirm,
            String name,
            String role) throws Exception {
        return createRequest(phone, password, passwordConfirm, name, role, true, true, false);
    }

    private SignupRequestDTO createRequest(
            String phone,
            String password,
            String passwordConfirm,
            String name,
            String role,
            Boolean serviceTermsAgreed,
            Boolean privacyTermsAgreed,
            Boolean marketingTermsAgreed) throws Exception {
        SignupRequestDTO request = new SignupRequestDTO();
        setField(request, "phone", phone);
        setField(request, "password", password);
        setField(request, "passwordConfirm", passwordConfirm);
        setField(request, "name", name);
        setField(request, "role", role);
        setField(request, "serviceTermsAgreed", serviceTermsAgreed);
        setField(request, "privacyTermsAgreed", privacyTermsAgreed);
        setField(request, "marketingTermsAgreed", marketingTermsAgreed);
        return request;
    }

    private LoginRequestDTO createLoginRequest(String phone, String password) throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        setField(request, "phone", phone);
        setField(request, "password", password);
        return request;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
