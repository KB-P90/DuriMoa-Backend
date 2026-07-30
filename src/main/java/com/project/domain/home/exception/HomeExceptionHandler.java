package com.project.domain.home.exception;

import com.project.common.response.ApiResponse;
import com.project.domain.home.controller.HomeController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = HomeController.class)
public class HomeExceptionHandler {

    // 홈 API의 처리되지 않은 오류를 민감정보 없이 기록하기 위한 로거입니다.
    private static final Logger log = LogManager.getLogger(HomeExceptionHandler.class);

    // 홈 도메인 예외를 공통 오류 응답 형식으로 변환합니다.
    @ExceptionHandler(HomeException.class)
    public ResponseEntity<ApiResponse<Void>> handleHomeException(HomeException exception) {
        return ResponseEntity
                .status(exception.getStatus())
                .body(ApiResponse.error(exception.getMessage()));
    }

    // 홈 API의 예상하지 못한 서버 오류를 안전한 메시지로 변환합니다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled home exception: {}", exception.getClass().getSimpleName(), exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버 처리 중 오류가 발생했습니다."));
    }
}
