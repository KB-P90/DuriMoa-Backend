package com.project.domain.strategy.card.exception;

import com.project.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class CardAuthenticationRequiredException extends ApiException {

    // 인증 사용자 식별 실패를 401 오류로 전달합니다.
    public CardAuthenticationRequiredException() {
        super(HttpStatus.UNAUTHORIZED, "인증 사용자 정보를 확인할 수 없습니다.");
    }
}
