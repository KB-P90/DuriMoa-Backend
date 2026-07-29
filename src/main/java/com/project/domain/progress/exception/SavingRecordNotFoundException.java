package com.project.domain.progress.exception;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;

public class SavingRecordNotFoundException extends CustomException {

    public SavingRecordNotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND, "저축 거래 내역이 없습니다.");
    }
}
