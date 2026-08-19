package com.project.domain.notification.domain;

import lombok.Getter;

@Getter
public enum NotificationType {

    GOAL_CREATED("결혼 시안이 생성되었습니다."),
    GOAL_CHANGED("결혼 시안이 수정되었습니다."),
    GOAL_APPROVAL_REQUESTED("결혼 시안 승인 요청이 도착했습니다."),
    GOAL_APPROVED("결혼 시안이 승인되었습니다."),
    GOAL_REJECTED("결혼 시안이 반려되었습니다."),
    COUPLE_REQUESTED("커플 연결을 요청받았습니다."),
    COUPLE_ACCEPTED("커플 연결 요청이 수락되었습니다."),
    COUPLE_DISCONNECTED("커플 연결이 해제되었습니다.");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }
}
