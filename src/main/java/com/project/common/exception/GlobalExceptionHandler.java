package com.project.common.exception;

import com.project.common.response.ApiResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 민감정보가 섞일 수 있는 요청 원문 대신 예외 타입과 안전한 메시지만 기록합니다.
    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);

    // 비즈니스 예외를 공통 오류 응답으로 변환합니다.
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
        HttpStatus status = exception.getStatus();
        return ResponseEntity
                .status(status)
                .body(ApiResponse.failure(status.value(), exception.getMessage()));
    }

    // 필수 요청 파라미터 누락을 검증 오류로 변환합니다.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(
            MissingServletRequestParameterException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure(HttpStatus.BAD_REQUEST.value(), "필수 요청값이 누락되었습니다."));
    }

    // 처리되지 않은 서버 오류를 민감정보 없는 공통 응답으로 변환합니다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled API exception: {}", exception.getClass().getSimpleName(), exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 처리 중 오류가 발생했습니다."));
    }
}
