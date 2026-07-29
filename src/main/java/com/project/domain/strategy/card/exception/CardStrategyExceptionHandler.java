package com.project.domain.strategy.card.exception;

import com.project.common.response.ApiResponse;
import com.project.domain.strategy.card.controller.CardStrategyController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CardStrategyController.class)
public class CardStrategyExceptionHandler {

    // 카드 추천 API의 예상하지 못한 오류를 안전하게 기록하기 위한 로거입니다.
    private static final Logger log = LogManager.getLogger(CardStrategyExceptionHandler.class);

    // 카드 추천 도메인 예외를 공통 오류 응답 형식으로 변환합니다.
    @ExceptionHandler(CardStrategyException.class)
    public ResponseEntity<ApiResponse<Void>> handleCardStrategyException(CardStrategyException exception) {
        return ResponseEntity
                .status(exception.getStatus())
                .body(ApiResponse.error(exception.getMessage()));
    }

    // 카드 추천 API의 처리되지 않은 서버 오류를 민감정보 없는 응답으로 변환합니다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled card strategy exception: {}", exception.getClass().getSimpleName(), exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버 처리 중 오류가 발생했습니다."));
    }
}
