package com.project.common.exception;

import com.project.common.response.ApiResponse;
import com.project.domain.auth.exception.PasswordConfirmationMismatchException;
import com.project.domain.auth.exception.PhoneDuplicateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PhoneDuplicateException.class)
    public ResponseEntity<ApiResponse<Void>> handlePhoneDuplicate(
            PhoneDuplicateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(PasswordConfirmationMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handlePasswordConfirmationMismatch(
            PasswordConfirmationMismatchException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("입력값을 확인해 주세요.");
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }
}
