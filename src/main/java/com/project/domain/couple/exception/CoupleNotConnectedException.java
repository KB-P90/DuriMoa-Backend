package com.project.domain.couple.exception;

import com.project.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class CoupleNotConnectedException extends BusinessException {

    public CoupleNotConnectedException(Long userId) {
        super("COUPLE_NOT_CONNECTED", "연결된 커플만 이용할 수 있습니다.", HttpStatus.FORBIDDEN);
    }
}
