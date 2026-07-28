package com.project.domain.strategy.card.exception;

import com.project.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class CardNotFoundException extends ApiException {

    // 카드 또는 활성 커플 조회 실패를 404 오류로 전달합니다.
    public CardNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
