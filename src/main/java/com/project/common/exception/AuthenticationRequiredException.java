package com.project.common.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationRequiredException extends BusinessException {

    public AuthenticationRequiredException() {
        super("AUTHENTICATION_REQUIRED", "인증된 사용자 정보가 필요합니다.", HttpStatus.UNAUTHORIZED);
    }
}
