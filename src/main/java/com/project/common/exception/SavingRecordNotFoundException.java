package com.project.common.exception;

import org.springframework.http.HttpStatus;

public class SavingRecordNotFoundException extends BusinessException {

    public SavingRecordNotFoundException() {
        super("SAVING_RECORD_NOT_FOUND", "저축 거래 내역이 없습니다.", HttpStatus.NOT_FOUND);
    }
}
