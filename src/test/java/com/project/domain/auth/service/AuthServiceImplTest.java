package com.project.domain.auth.service;

import com.project.domain.auth.dto.SignupRequestDTO;
import com.project.domain.auth.dto.SignupResponseDTO;
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

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(authMapper, passwordEncoder);
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

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
