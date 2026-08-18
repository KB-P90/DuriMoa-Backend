package com.project.common.enums;

import lombok.Getter;

@Getter
public enum MissionStatus {

    CHALLENGE("도전하기"),
    IN_PROGRESS("도전중"),
    SUCCESS("성공"),
    FAILURE("실패"),
    EXPIRED("만료");

    private final String status;

    MissionStatus(String status) {
        this.status = status;
    }
}
