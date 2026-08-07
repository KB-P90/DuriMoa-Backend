package com.project.domain.auth.controller;

import com.project.common.response.ApiResponse;
import com.project.common.security.RefreshTokenCookieProvider;
import com.project.domain.auth.dto.LoginRequestDTO;
import com.project.domain.auth.dto.LoginResponseDTO;
import com.project.domain.auth.dto.LoginResultDTO;
import com.project.domain.auth.dto.SignupRequestDTO;
import com.project.domain.auth.dto.SignupResponseDTO;
import com.project.domain.auth.dto.TokenRefreshResponseDTO;
import com.project.domain.auth.dto.TokenRefreshResultDTO;
import com.project.domain.auth.service.AuthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Api(tags = "Auth", description = "인증 및 회원가입 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

    @ApiOperation(value = "회원가입", notes = "로컬 회원을 등록하고 비밀번호를 BCrypt로 암호화합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponseDTO>> signup(
            @Valid @RequestBody SignupRequestDTO request) {
        SignupResponseDTO response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", response));
    }

    @ApiOperation(
            value = "로그인",
            notes = "휴대폰 번호와 비밀번호를 검증하고 Access Token과 Refresh Token 쿠키를 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request) {
        LoginResultDTO result = authService.login(request);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookieProvider.createCookie(result.getRefreshToken()).toString())
                .body(ApiResponse.success("로그인이 완료되었습니다.", result.getResponse()));
    }

    @ApiOperation(
            value = "Access Token 재발급",
            notes = "Refresh Token 쿠키를 검증하고 Access Token과 Refresh Token을 재발급합니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponseDTO>> refresh(
            @CookieValue(
                    value = RefreshTokenCookieProvider.COOKIE_NAME,
                    required = false) String refreshToken) {
        TokenRefreshResultDTO result = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookieProvider.createCookie(result.getRefreshToken()).toString())
                .body(ApiResponse.success(
                        "Access Token이 재발급되었습니다.",
                        result.getResponse()));
    }

    @ApiOperation(
            value = "로그아웃",
            notes = "Redis의 Refresh Token을 삭제하고 Refresh Token 쿠키를 만료시킵니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(
                    value = RefreshTokenCookieProvider.COOKIE_NAME,
                    required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookieProvider.deleteCookie().toString())
                .body(ApiResponse.success("로그아웃이 완료되었습니다."));
    }
}
