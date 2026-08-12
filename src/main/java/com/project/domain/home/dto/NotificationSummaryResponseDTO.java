package com.project.domain.home.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationSummaryResponseDTO {

    // 홈 상단 알림 버튼에 표시할 읽지 않은 알림 개수입니다.
    @JsonProperty("unread_count")
    private final Integer unreadCount;

    // 홈 상단 알림 버튼에 새 알림 표시가 필요한지 나타냅니다.
    @JsonProperty("has_unread")
    private final Boolean hasUnread;
}
