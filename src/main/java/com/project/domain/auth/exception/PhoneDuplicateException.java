package com.project.domain.auth.exception;

public class PhoneDuplicateException extends RuntimeException {

    public PhoneDuplicateException() {
        super("이미 가입된 휴대폰 번호입니다.");
    }
}
