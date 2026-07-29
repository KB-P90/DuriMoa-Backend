package com.project.domain.couple.exception;

import com.project.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class CoupleAccessDeniedException extends BusinessException {

    public CoupleAccessDeniedException(Long userId, Long coupleId) {
        super("COUPLE_ACCESS_DENIED", "커플 구성원만 이용할 수 있습니다.", HttpStatus.FORBIDDEN);
    }
}
