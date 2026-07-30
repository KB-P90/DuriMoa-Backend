package com.project.domain.codef.exception;

public class CodefTokenException extends RuntimeException {

    public CodefTokenException(String message) {
        super(message);
    }

    public CodefTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
