package com.project.domain.progress.exception;

import com.project.common.response.ApiResponse;
import com.project.domain.progress.controller.ProgressController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = ProgressController.class)
public class ProgressExceptionHandler {

    @ExceptionHandler(SavingRecordNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSavingRecordNotFound(
            SavingRecordNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ApiResponse.error(exception.getReason()));
    }
}
