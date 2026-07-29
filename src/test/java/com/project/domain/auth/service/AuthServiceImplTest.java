package com.project.domain.auth.service;

import com.project.common.security.JwtTokenProvider;
import com.project.domain.auth.dto.LoginRequestDTO;
import com.project.domain.auth.dto.LoginResponseDTO;
import com.project.domain.auth.dto.LoginUserDTO;
import com.project.domain.auth.dto.SignupRequestDTO;
import com.project.domain.auth.dto.SignupResponseDTO;
import com.project.domain.auth.exception.InvalidLoginCredentialsException;
import com.project.domain.auth.exception.PasswordConfirmationMismatchException;
import com.project.domain.auth.exception.PhoneDuplicateException;
import com.project.domain.auth.mapper.AuthMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(authMapper, passwordEncoder, jwtTokenProvider);
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
        assertEquals(1L, response.getUserId());
        assertEquals("01012345678", response.getPhone());
        assertEquals("LOCAL", response.getProvider());
    }

    @Test
    void signupRejectsDuplicatePhone() throws Exception {
        SignupRequestDTO request =
                createRequest("01012345678", "password123", "password123", "홍길동", "G");
        when(authMapper.existsUserByPhone("01012345678")).thenReturn(true);

        assertThrows(PhoneDuplicateException.class, () -> authService.signup(request));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void signupRejectsPasswordConfirmationMismatch() throws Exception {
        SignupRequestDTO request =
                createRequest("01012345678", "password123", "different123", "홍길동", "G");

        assertThrows(
                PasswordConfirmationMismatchException.class,
                () -> authService.signup(request));
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

        LoginResponseDTO response = authService.login(request);

        assertEquals("accessToken", response.getAccessToken());
        assertEquals(1L, response.getUser().getUserId());
        assertEquals("01012345678", response.getUser().getPhone());
        assertEquals("LOCAL", response.getUser().getProvider());
    }

    @Test
    void loginRejectsUnknownPhone() throws Exception {
        LoginRequestDTO request = createLoginRequest("01012345678", "password123");
        when(authMapper.findLoginUserByPhone("01012345678")).thenReturn(null);

        assertThrows(InvalidLoginCredentialsException.class, () -> authService.login(request));
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

        assertThrows(InvalidLoginCredentialsException.class, () -> authService.login(request));
        verify(jwtTokenProvider, never()).createAccessToken(1L, "G");
    }

    private SignupRequestDTO createRequest(
            String phone,
            String password,
            String passwordConfirm,
            String name,
            String role) throws Exception {
        SignupRequestDTO request = new SignupRequestDTO();
        setField(request, "phone", phone);
        setField(request, "password", password);
        setField(request, "passwordConfirm", passwordConfirm);
        setField(request, "name", name);
        setField(request, "role", role);
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
