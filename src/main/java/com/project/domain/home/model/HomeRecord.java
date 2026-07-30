package com.project.domain.home.model;

import lombok.Getter;
import lombok.Setter;

public final class HomeRecord {

    // 홈 조회 DB 결과 모델을 묶기 위한 클래스입니다.
    private HomeRecord() {
    }

    @Getter
    @Setter
    public static class HomeUserRecord {

        // 인증 사용자 ID입니다.
        private Long userId;

        // 인증 사용자의 화면 표시 이름입니다.
        private String userName;

        // 인증 사용자의 신랑·신부 역할입니다.
        private String userRole;
    }

    @Getter
    @Setter
    public static class HomeCoupleRecord {

        // 인증 사용자가 속한 활성 커플 ID입니다.
        private Long coupleId;

        // 활성 커플의 신랑 사용자 ID입니다.
        private Long groomId;

        // 활성 커플의 신부 사용자 ID입니다.
        private Long brideId;

        // 인증 사용자 ID입니다.
        private Long userId;

        // 인증 사용자의 화면 표시 이름입니다.
        private String userName;

        // 인증 사용자의 신랑·신부 역할입니다.
        private String userRole;

        // 활성 커플의 상대방 사용자 ID입니다.
        private Long partnerUserId;

        // 활성 커플의 상대방 화면 표시 이름입니다.
        private String partnerName;

        // 활성 커플의 상대방 신랑·신부 역할입니다.
        private String partnerRole;

        // 활성 커플 연결 상태입니다.
        private String coupleStatus;
    }

    @Getter
    @Setter
    public static class HomeChecklistEvidenceRecord {

        // 인증 사용자에게 연결 계좌 또는 카드가 있는지 나타내는 DB 조회 결과입니다.
        private Boolean accountLinked;

        // 활성 커플의 대표 결혼 목표가 있는지 나타내는 DB 조회 결과입니다.
        private Boolean weddingGoalCreated;

        // 대표 결혼 목표에 지역 값이 있는지 나타내는 DB 조회 결과입니다.
        private Boolean regionSelected;

        // 활성 커플 구성원의 양수 저축 기록이 있는지 나타내는 DB 조회 결과입니다.
        private Boolean firstSavingRecorded;

        // 대표 결혼 목표 식별자입니다.
        private Long mainGoalId;

        // 대표 결혼 목표 상태입니다.
        private String mainGoalStatus;

        // 대표 결혼 목표의 결혼 예정일입니다.
        private String weddingDate;

        // 대표 결혼 목표의 지역 값입니다.
        private String region;
    }

    @Getter
    @Setter
    public static class HomeSavingMissionSummaryRecord {

        // 현재 월 절약 미션들의 예상 절약 금액 합계입니다.
        private Long totalExpectedSavingAmount;

        // 현재 월에 도전 중인 절약 미션 수입니다.
        private Integer inProgressMissionCount;

        // 현재 월에 완료한 절약 미션 수입니다.
        private Integer completedMissionCount;

        // 홈에 대표로 노출할 절약 미션 ID입니다.
        private Long missionId;

        // 대표 절약 미션의 카테고리 코드입니다.
        private String categoryCode;

        // 대표 절약 미션의 제목입니다.
        private String title;

        // 대표 절약 미션의 상태입니다.
        private String status;

        // 대표 절약 미션의 예상 절약 금액입니다.
        private Long expectedSavingAmount;
    }
}
