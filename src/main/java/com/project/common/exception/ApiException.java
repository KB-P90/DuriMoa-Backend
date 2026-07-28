package com.project.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    // 응답에 사용할 HTTP 상태입니다.
    private final HttpStatus status;

    // 비즈니스 예외 상태와 메시지를 함께 보관합니다.
    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
