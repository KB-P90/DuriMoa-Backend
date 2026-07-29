package com.project.domain.auth.exception;

public class InvalidLoginCredentialsException extends RuntimeException {

    public InvalidLoginCredentialsException() {
        super("휴대폰 번호 또는 비밀번호가 일치하지 않습니다.");
    }
}
