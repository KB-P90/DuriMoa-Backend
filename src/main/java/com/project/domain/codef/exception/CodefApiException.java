package com.project.domain.codef.exception;

public class CodefApiException extends RuntimeException {

    public CodefApiException(String message) {
        super(message);
    }

    public CodefApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
