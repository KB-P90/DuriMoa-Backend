package com.project.domain.codef.exception;

import com.project.common.response.ApiResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Log4j2
@RestControllerAdvice(basePackages = "com.project.domain.codef")
public class CodefExceptionHandler {

    @ExceptionHandler(CodefApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleCodefApiException(CodefApiException e) {
        log.error("CODEF API 오류 발생: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("CODEF 연동 오류: " + e.getMessage()));
    }

    @ExceptionHandler(CodefTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleCodefTokenException(CodefTokenException e) {
        log.error("CODEF 토큰 인증 오류 발생: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("CODEF 토큰 오류: " + e.getMessage()));
    }
}
