package com.project.common.security.exception;

import com.project.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class AuthenticationRequiredException extends BusinessException {

    public AuthenticationRequiredException() {
        super("AUTHENTICATION_REQUIRED", "인증된 사용자 정보가 필요합니다.", HttpStatus.UNAUTHORIZED);
    }
}
