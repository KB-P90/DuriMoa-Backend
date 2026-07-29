package com.project.domain.couple.exception;

import com.project.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class CoupleNotFoundException extends BusinessException {

    public CoupleNotFoundException(Long userId) {
        super("COUPLE_NOT_FOUND", "사용자와 연결된 커플을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
    }
}
