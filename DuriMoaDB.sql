-- KB 두리모아 ERD 기반 MySQL 8.0 DDL
-- Charset: utf8mb4

CREATE DATABASE IF NOT EXISTS DuriMoaDB;
USE DuriMoaDB;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `record`;
DROP TABLE IF EXISTS `goal_item`;
DROP TABLE IF EXISTS `goal`;
DROP TABLE IF EXISTS `card`;
DROP TABLE IF EXISTS `card_record`;
DROP TABLE IF EXISTS `account`;
DROP TABLE IF EXISTS `account_record`;
DROP TABLE IF EXISTS `service_record`;
DROP TABLE IF EXISTS `last_update`;
DROP TABLE IF EXISTS `saving_missions`;
DROP TABLE IF EXISTS `notification`;
DROP TABLE IF EXISTS `notification_setting`;
DROP TABLE IF EXISTS `terms_agreement`;
DROP TABLE IF EXISTS `invitation`;
DROP TABLE IF EXISTS `couple`;
DROP TABLE IF EXISTS `card_product`;
DROP TABLE IF EXISTS `wedding_statistics`;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `codef_access_token`;
-- DROP TABLE IF EXISTS `expense_avg`;

SET FOREIGN_KEY_CHECKS = 1;

-- 사용자
CREATE TABLE `user` (
                        `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '사용자 ID',
                        `phone`         VARCHAR(20)  NOT NULL COMMENT '핸드폰 번호',
                        `password`      VARCHAR(255) NULL COMMENT '암호화된 비밀번호; 소셜 로그인 사용자는 NULL 가능',
                        `name`          VARCHAR(50)  NOT NULL COMMENT '이름',
                        `role`          VARCHAR(10)  NOT NULL COMMENT '역할: B(신부), G(신랑)',
                        `profile_image` LONGBLOB     NULL COMMENT '프로필 사진 원본',
                        `provider`      VARCHAR(10)  NOT NULL DEFAULT 'LOCAL' COMMENT 'LOCAL, KAKAO, NAVER',
                        `income`        BIGINT       NOT NULL DEFAULT 0 COMMENT '연소득',
                        `asset`         BIGINT       NOT NULL DEFAULT 0 COMMENT '총자산',
                        `connected_id`     VARCHAR(255) NULL COMMENT '커넥티드 ID',
                        `is_shared`     BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '상대방 공유 여부',
                        `is_deleted`    BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '회원 탈퇴 여부',
                        `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_user_phone` (`phone`)
);

-- 약관 동의
CREATE TABLE `terms_agreement` (
                                   `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '약관 동의 ID',
                                   `user_id`    BIGINT      NOT NULL COMMENT '사용자 ID',
                                   `terms_type` VARCHAR(50) NOT NULL COMMENT '약관 유형',
                                   `agreed`     BOOLEAN     NOT NULL DEFAULT FALSE COMMENT '동의 여부',
                                   `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_terms_agreement_user_type` (`user_id`, `terms_type`),
                                   KEY `idx_terms_agreement_user_id` (`user_id`),
                                   CONSTRAINT `fk_terms_agreement_user`
                                       FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
                                           ON UPDATE CASCADE ON DELETE CASCADE
);

-- 알림 설정
CREATE TABLE `notification_setting` (
                                        `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '알림 설정 ID',
                                        `user_id`         BIGINT      NOT NULL COMMENT '사용자 ID',
                                        `type`            VARCHAR(10) NOT NULL COMMENT '알림 종류',
                                        `is_push_enabled` BOOLEAN     NOT NULL DEFAULT TRUE COMMENT '푸시 알림 여부',
                                        `created_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                                        PRIMARY KEY (`id`),
                                        UNIQUE KEY `uk_notification_setting_user_type` (`user_id`, `type`),
                                        KEY `idx_notification_setting_user_id` (`user_id`),
                                        CONSTRAINT `fk_notification_setting_user`
                                            FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
                                                ON UPDATE CASCADE ON DELETE CASCADE
);

-- 알림
CREATE TABLE `notification` (
                                `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '알림 ID',
                                `user_id`    BIGINT       NOT NULL COMMENT '사용자 ID',
                                `type`       VARCHAR(10)  NOT NULL COMMENT '알림 종류',
                                `title`      VARCHAR(100) NOT NULL COMMENT '제목',
                                `content`    TEXT         NOT NULL COMMENT '내용',
                                `is_read`    BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '읽음 여부',
                                `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                                PRIMARY KEY (`id`),
                                KEY `idx_notification_user_created_at` (`user_id`, `created_at`),
                                KEY `idx_notification_user_is_read` (`user_id`, `is_read`),
                                CONSTRAINT `fk_notification_user`
                                    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
                                        ON UPDATE CASCADE ON DELETE CASCADE
);

-- 초대 코드
CREATE TABLE `invitation` (
                              `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '초대 ID',
                              `inviter_id`  BIGINT      NOT NULL COMMENT '초대한 사용자 ID',
                              `invitee_id`  BIGINT      NULL COMMENT '초대받은 사용자 ID',
                              `invite_code` VARCHAR(30) NOT NULL COMMENT '초대 코드',
                              `status`      VARCHAR(30) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED, REQUESTED, ACCEPTED, REJECTED, EXPIRED',
                              `expired_at`  DATETIME    NOT NULL COMMENT '만료 일시',
                              `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_invitation_inviter_invitee` (`inviter_id`, `invitee_id`),
                              KEY `idx_invitation_invite_code` (`invite_code`),
                              KEY `idx_invitation_inviter_id` (`inviter_id`),
                              KEY `idx_invitation_invitee_id` (`invitee_id`),
                              KEY `idx_invitation_status_expired_at` (`status`, `expired_at`),
                              CONSTRAINT `fk_invitation_inviter`
                                  FOREIGN KEY (`inviter_id`) REFERENCES `user` (`id`)
                                      ON UPDATE CASCADE ON DELETE CASCADE,
                              CONSTRAINT `fk_invitation_invitee`
                                  FOREIGN KEY (`invitee_id`) REFERENCES `user` (`id`)
                                      ON UPDATE CASCADE ON DELETE SET NULL
);

-- 커플
CREATE TABLE `couple` (
                          `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '커플 ID',
                          `groom_id`   BIGINT      NOT NULL COMMENT '신랑 사용자 ID',
                          `bride_id`   BIGINT      NOT NULL COMMENT '신부 사용자 ID',
                          `status`     VARCHAR(30) NOT NULL DEFAULT 'WAIT' COMMENT 'WAIT, CONNECTED, DISCONNECTED',
                          `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                          PRIMARY KEY (`id`),
                          UNIQUE KEY `uk_couple_groom_bride` (`groom_id`, `bride_id`),
                          KEY `idx_couple_groom_id` (`groom_id`),
                          KEY `idx_couple_bride_id` (`bride_id`),
                          CONSTRAINT `fk_couple_groom`
                              FOREIGN KEY (`groom_id`) REFERENCES `user` (`id`)
                                  ON UPDATE CASCADE ON DELETE RESTRICT,
                          CONSTRAINT `fk_couple_bride`
                              FOREIGN KEY (`bride_id`) REFERENCES `user` (`id`)
                                  ON UPDATE CASCADE ON DELETE RESTRICT
);

-- 목표
CREATE TABLE `goal` (
                        `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '목표 ID',
                        `couple_id`    BIGINT      NOT NULL COMMENT '커플 ID',
                        `name`         VARCHAR(50) NOT NULL COMMENT '시안 제목',
                        `wedding_date` DATE        NOT NULL COMMENT '결혼식 날짜',
                        `region`       VARCHAR(50) NOT NULL COMMENT '결혼식 지역',
                        `total_budget` BIGINT      NOT NULL DEFAULT 0 COMMENT '총 예산 비용',
                        `groom_ratio`  BIGINT      NOT NULL DEFAULT 50 COMMENT '신랑 분담 비율',
                        `bride_ratio`  BIGINT      NOT NULL DEFAULT 50 COMMENT '신부 분담 비율',
                        `status`       VARCHAR(30) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL, PENDING, CONFIRMED',
                        `is_main`      BOOLEAN     NOT NULL DEFAULT FALSE COMMENT '공동 목표 여부',
                        `requested_by` BIGINT      NULL COMMENT '메인 시안 승인 요청자 ID',
                        `requested_at` DATETIME    NULL COMMENT '메인 시안 승인 요청 일시',
                        `created_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                        PRIMARY KEY (`id`),
                        KEY `idx_goal_couple_id` (`couple_id`),
                        KEY `idx_goal_couple_main` (`couple_id`, `is_main`),
                        KEY `idx_goal_couple_status` (`couple_id`, `status`),
                        KEY `idx_goal_requested_by` (`requested_by`),
                        CONSTRAINT `fk_goal_couple`
                            FOREIGN KEY (`couple_id`) REFERENCES `couple` (`id`)
                                ON UPDATE CASCADE ON DELETE CASCADE,
                        CONSTRAINT `fk_goal_requested_by`
                            FOREIGN KEY (`requested_by`) REFERENCES `user` (`id`)
                                ON UPDATE CASCADE ON DELETE SET NULL,
                        CONSTRAINT `chk_goal_total_budget`
                            CHECK (`total_budget` >= 0)
);

-- 목표 항목
CREATE TABLE `goal_item` (
                             `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '목표 항목 ID',
                             `goal_id`       BIGINT      NOT NULL COMMENT '목표 ID',
                             `category`      VARCHAR(30) NOT NULL COMMENT '카테고리',
                             `target_amount` BIGINT      NOT NULL DEFAULT 0 COMMENT '타깃 금액',
                             `is_included`   BOOLEAN     NOT NULL DEFAULT TRUE COMMENT '예산 포함 여부',
                             `is_completed`  BOOLEAN     NOT NULL DEFAULT FALSE COMMENT '완료 여부',
                             `type`          VARCHAR(30) NULL COMMENT '유형: 알뜰형, 균형형, 플렉스형 등',
                             `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                             PRIMARY KEY (`id`),
                             KEY `idx_goal_item_goal_id` (`goal_id`),
                             KEY `idx_goal_item_goal_category` (`goal_id`, `category`),
                             CONSTRAINT `fk_goal_item_goal`
                                 FOREIGN KEY (`goal_id`) REFERENCES `goal` (`id`)
                                     ON UPDATE CASCADE ON DELETE CASCADE,
                             CONSTRAINT `chk_goal_item_target_amount`
                                 CHECK (`target_amount` >= 0)
);

-- 카드 상품
CREATE TABLE `card_product` (
                                `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '카드 상품 ID',
                                `company`    VARCHAR(50)  NOT NULL COMMENT '카드사',
                                `name`       VARCHAR(100) NOT NULL COMMENT '카드 이름',
                                `annual_fee` INT          NOT NULL DEFAULT 0 COMMENT '연회비',
                                `image`      VARCHAR(255) NULL COMMENT '카드 이미지 경로 또는 URL',
                                `benefit`    JSON         NULL COMMENT '혜택 목록',
                                `condition`  VARCHAR(255) NULL COMMENT '이용 조건',
                                `updated_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '최종 변경일',
                                `created_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',

                                PRIMARY KEY (`id`),
                                KEY `idx_card_product_company` (`company`),
                                CONSTRAINT `chk_card_product_annual_fee`
                                    CHECK (`annual_fee` >= 0)
);

-- 사용자 계좌 정보
CREATE TABLE `account` (
                           `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '계좌/카드 정보 ID',
                           `user_id`          BIGINT       NOT NULL COMMENT '사용자 ID',
                           `company`          VARCHAR(50)  NOT NULL COMMENT '회사/금융기관',
                           `number`           VARCHAR(50)  NOT NULL COMMENT '계좌 번호',
                           `account_name`        VARCHAR(100) NULL COMMENT '계좌 이름',
                           `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                           PRIMARY KEY (`id`),
                           KEY `idx_account_user_id` (`user_id`),
                           CONSTRAINT `fk_account_user`
                               FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
                                   ON UPDATE CASCADE ON DELETE CASCADE
);

-- 사용자 카드 정보
CREATE TABLE `card` (
                           `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '계좌/카드 정보 ID',
                           `user_id`          BIGINT       NOT NULL COMMENT '사용자 ID',
                           `card_product_id`  BIGINT       NULL COMMENT '카드 상품 ID',
                           `company`          VARCHAR(50)  NOT NULL COMMENT '회사/금융기관',
                           `number`           VARCHAR(50)  NOT NULL COMMENT '카드 번호',
                           `card_name`        VARCHAR(100) NULL COMMENT '카드 이름',
                           `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                           PRIMARY KEY (`id`),
                           KEY `idx_card_user_id` (`user_id`),
                           KEY `idx_card_product_id` (`card_product_id`),
                           CONSTRAINT `fk_card_user`
                               FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
                                   ON UPDATE CASCADE ON DELETE CASCADE,
                           CONSTRAINT `fk_card_product`
                               FOREIGN KEY (`card_product_id`) REFERENCES `card_product` (`id`)
                                   ON UPDATE CASCADE ON DELETE SET NULL
);

-- 카드 원본 거래 내역
CREATE TABLE `card_record` (
                          `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '기록 ID',
                          `card_id`  BIGINT       NOT NULL COMMENT '카드 정보 ID',
                          `type`        VARCHAR(10)  NOT NULL COMMENT 'INCOME, EXPENSE, SAVING',
                          `category`    VARCHAR(10)  NOT NULL COMMENT '결혼비용, 개인소비 등',
                          `title`       VARCHAR(100) NOT NULL COMMENT '제목',
                          `amount`      BIGINT       NOT NULL COMMENT '금액',
                          `record_date` DATE         NOT NULL COMMENT '거래 일자',
                          `memo`        TEXT         NULL COMMENT '메모',
                          `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                          PRIMARY KEY (`id`),
                          KEY `idx_record_card_date` (`card_id`, `record_date`),
                          KEY `idx_record_type_date` (`type`, `record_date`),
                          CONSTRAINT `fk_record_card`
                              FOREIGN KEY (`card_id`) REFERENCES `card` (`id`)
                                  ON UPDATE CASCADE ON DELETE CASCADE,
                          CONSTRAINT `chk_card_record_amount`
                              CHECK (`amount` >= 0)
);

-- 계좌 원본 거래 내역
CREATE TABLE `account_record` (
                          `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '기록 ID',
                          `account_id`  BIGINT       NOT NULL COMMENT '계좌/카드 정보 ID',
                          `type`        VARCHAR(10)  NOT NULL COMMENT 'INCOME, EXPENSE, SAVING',
                          `category`    VARCHAR(10)  NOT NULL COMMENT '결혼비용, 개인소비 등',
                          `title`       VARCHAR(100) NOT NULL COMMENT '제목',
                          `amount`      BIGINT       NOT NULL COMMENT '금액',
                          `record_date` DATE         NOT NULL COMMENT '거래 일자',
                          `memo`        TEXT         NULL COMMENT '메모',
                          `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                          PRIMARY KEY (`id`),
                          KEY `idx_record_account_date` (`account_id`, `record_date`),
                          KEY `idx_record_type_date` (`type`, `record_date`),
                          CONSTRAINT `fk_record_account`
                              FOREIGN KEY (`account_id`) REFERENCES `account` (`id`)
                                  ON UPDATE CASCADE ON DELETE CASCADE,
                          CONSTRAINT `chk_account_record_amount`
                              CHECK (`amount` >= 0)
);

-- 서비스 거래 내역
CREATE TABLE `service_record` (
                          `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '기록 ID',
                          `user_id`     BIGINT       NOT NULL COMMENT '사용자 ID',
                          `type`        VARCHAR(10)      NOT NULL COMMENT '내역 구분',
                          `category`    VARCHAR(20)  NOT NULL COMMENT '결혼비용, 개인소비 등',
                          `changed_category`    VARCHAR(20)  NULL COMMENT '변경된 카테고리',
                          `amount`      BIGINT       NOT NULL COMMENT '금액',
                          `record_date` DATE         NOT NULL COMMENT '거래 일자',
                          `memo`        TEXT         NULL COMMENT '메모',
                          `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                          `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정 일시',

                          PRIMARY KEY (`id`),
                          KEY `idx_account_user_id` (`user_id`),
                          CONSTRAINT `fk_service_record_user`
                              FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
                                  ON UPDATE CASCADE ON DELETE CASCADE,
                          CONSTRAINT `chk_service_record_amount`
                              CHECK (`amount` >= 0)
);

-- 마지막 갱신
CREATE TABLE `last_update` (
                                  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '기록 ID',
                                  `user_id`     BIGINT       NOT NULL COMMENT '사용자 ID',
                                  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정 일시',

                                  PRIMARY KEY (`id`),
                                  KEY `idx_account_user_id` (`user_id`),
                                  CONSTRAINT `fk_last_update_user`
                                      FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
                                          ON UPDATE CASCADE ON DELETE CASCADE
);

-- 절약 미션
CREATE TABLE `saving_missions` (
                                   `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '절약 미션 ID',
                                   `user_id`         BIGINT       NOT NULL COMMENT '사용자 ID',
                                   `category`        VARCHAR(50)  NOT NULL COMMENT '지출 카테고리',
                                   `title`           VARCHAR(100) NOT NULL COMMENT '미션 제목',
                                   `expected_saving` BIGINT       NOT NULL DEFAULT 0 COMMENT '예상 절감액',
                                   `status`          VARCHAR(10)  NOT NULL COMMENT '미션 상태',
                                   `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                                   PRIMARY KEY (`id`),
                                   KEY `idx_saving_missions_user_id` (`user_id`),
                                   KEY `idx_saving_missions_user_status` (`user_id`, `status`),
                                   CONSTRAINT `fk_saving_missions_user`
                                       FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
                                           ON UPDATE CASCADE ON DELETE CASCADE,
                                   CONSTRAINT `chk_saving_missions_expected_saving`
                                       CHECK (`expected_saving` >= 0)
);

-- 결혼 통계
CREATE TABLE `wedding_statistics` (
                                      `id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '통계 ID',
                                      `region`   VARCHAR(100) NOT NULL COMMENT '지역',
                                      `category` VARCHAR(30)  NOT NULL COMMENT '웨딩 카테고리',
                                      `high_10`  BIGINT       NOT NULL COMMENT '상위 10 기준 금액',
                                      `high_25`  BIGINT       NOT NULL COMMENT '상위 25 기준 금액',
                                      `mid_50`   BIGINT       NOT NULL COMMENT '중간값',
                                      `low_25`   BIGINT       NOT NULL COMMENT '하위 25 기준 금액',
                                      `low_10`   BIGINT       NOT NULL COMMENT '하위 10 기준 금액',

                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_wedding_statistics_region_category` (`region`, `category`),
                                      CONSTRAINT `chk_wedding_statistics_amounts`
                                          CHECK (`high_10` >= 0 AND `high_25` >= 0 AND `mid_50` >= 0 AND `low_25` >= 0 AND `low_10` >= 0)
);

-- CODEF 토큰
CREATE TABLE `codef_access_token` (
                                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                    access_token TEXT NOT NULL,
                                    token_type VARCHAR(20),
                                    expires_at DATETIME NOT NULL,
                                    created_at DATETIME NOT NULL
);

/*

CREATE TABLE expense_avg (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    category VARCHAR(50) NOT NULL
        COMMENT '지출 카테고리 코드',

    avg_amount DECIMAL(15, 2) NOT NULL
        COMMENT '2030세대 카테고리별 월평균 지출 금액',

    sample_count INT NULL
        COMMENT '평균 산출에 사용된 표본 수',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_expense_avg_amount
        CHECK (avg_amount >= 0),

    CONSTRAINT chk_expense_avg_sample_count
        CHECK (sample_count IS NULL OR sample_count >= 0),

    UNIQUE KEY uk_expense_avg_category (category)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '2030세대 카테고리별 월평균 지출';

*/
