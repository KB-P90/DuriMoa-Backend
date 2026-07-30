package com.project.common.enums;

import lombok.Getter;

@Getter
public enum CoupleStatus {

    WAIT("WAIT"),
    CONNECTED("CONNECTED"),
    DISCONNECTED("DISCONNECTED");

    private final String status;

    CoupleStatus(String status) {
        this.status = status;
    }
}
