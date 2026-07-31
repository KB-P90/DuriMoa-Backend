package com.project.domain.home.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SavingAlertResponseDTO {

    // 표시할 홈 알림 유형은 알림 도메인 연결 후 설정합니다.
    @JsonProperty("alert_type")
    private final String alertType;

    // 홈 알림 제목은 실제 알림 조회 결과를 연결합니다.
    @JsonProperty("title")
    private final String title;

    // 홈 알림 내용은 실제 알림 조회 결과를 연결합니다.
    @JsonProperty("message")
    private final String message;

    // 알림 생성 시점은 알림 도메인 계약 확정 후 연결합니다.
    @JsonProperty("created_at")
    private final String createdAt;
}
