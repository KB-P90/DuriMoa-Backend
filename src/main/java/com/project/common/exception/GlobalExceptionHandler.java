package com.project.common.exception;

import com.project.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 커스텀 예외 처리
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        log.error("CustomException: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getMessage()));
    }

    /**
     * DTO 유효성 검사(@Valid) 및 입력 파라미터 관련 예외 처리 (HTTP 400 BAD_REQUEST)
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequestExceptions(Exception e) {
        String errorMessage = ErrorCode.INVALID_INPUT_VALUE.getMessage();

        if (e instanceof MethodArgumentNotValidException) {
            BindingResult bindingResult = ((MethodArgumentNotValidException) e).getBindingResult();
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null) {
                errorMessage = fieldError.getDefaultMessage();
            }
        } else if (e instanceof MethodArgumentTypeMismatchException) {
            errorMessage = "요청 파라미터의 타입이 올바르지 않습니다.";
        } else if (e instanceof HttpMessageNotReadableException) {
            errorMessage = "요청 바디 데이터의 형식이 올바르지 않습니다.";
        } else if (e instanceof MissingServletRequestParameterException) {
            errorMessage = "필수 요청 파라미터가 누락되었습니다.";
        }

        log.error("Input Validation Error (400 BAD_REQUEST): {}", errorMessage);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorMessage));
    }

    /**
     * 업로드 파일 용량 초과 예외 처리 (HTTP 400 BAD_REQUEST)
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("File Upload Size Exceeded (400 BAD_REQUEST): {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("업로드 가능한 파일 용량을 초과했습니다."));
    }

    /**
     * 알 수 없는 서버 내부 예외 처리 (Fallback HTTP 500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled System Exception (500 INTERNAL_SERVER_ERROR): ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
