package com.project.domain.auth.exception;

public class RequiredTermsAgreementException extends RuntimeException {

    public RequiredTermsAgreementException() {
        super("필수 약관에 동의해야 회원가입이 가능합니다.");
    }
}
