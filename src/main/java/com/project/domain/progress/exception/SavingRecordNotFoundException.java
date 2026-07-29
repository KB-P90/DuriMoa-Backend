package com.project.domain.progress.exception;

public class SavingRecordNotFoundException extends RuntimeException {

    public SavingRecordNotFoundException() {
        super("저축 거래 내역이 없습니다.");
    }
}
