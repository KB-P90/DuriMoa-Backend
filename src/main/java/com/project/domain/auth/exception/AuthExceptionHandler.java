package com.project.domain.auth.exception;

import com.project.common.response.ApiResponse;
import com.project.domain.auth.controller.AuthController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {

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

    @ExceptionHandler(RequiredTermsAgreementException.class)
    public ResponseEntity<ApiResponse<Void>> handleRequiredTermsAgreement(
            RequiredTermsAgreementException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(InvalidLoginCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidLoginCredentials(
            InvalidLoginCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(exception.getMessage()));
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
