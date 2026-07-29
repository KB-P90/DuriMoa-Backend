package com.project.common.enums;

import lombok.Getter;

@Getter
public enum MissionStatus {

    CHALLENGE("도전하기"),
    IN_PROGRESS("도전중"),
    COMPLETED("완료");

    private final String status;

    MissionStatus(String status) {
        this.status = status;
    }
}
