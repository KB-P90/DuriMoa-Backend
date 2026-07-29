package com.project.common.exception;

import com.project.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ApiResponse<Void> response = ApiResponse.error(
                exception.getStatus().value(),
                exception.getMessage()
        );
        return ResponseEntity.status(exception.getStatus()).body(response);
    }
}
