-- 홈 화면 API 로컬 검증용 더미 데이터입니다.
-- 공통 DDL은 수정하지 않고, 선택된 로컬 DB에 테스트 데이터만 추가합니다.
-- 애플리케이션 database.properties와 같은 로컬 DB를 선택합니다.

SET NAMES utf8mb4;
USE durimoadb;

SET @home_groom_phone = '01090010001';
SET @home_bride_phone = '01090010002';
SET @home_test_password = 'password123';
SET @home_test_password_hash = '$2a$10$OJneddDFnJ033zAO1pjLjOpg2SsYo70VannDag89uMh22503jmnl6';

INSERT INTO `user` (
    phone,
    password,
    name,
    role,
    provider,
    income,
    asset,
    is_shared,
    is_deleted
) VALUES (
    @home_groom_phone,
    @home_test_password_hash,
    '홈테스트신랑',
    'G',
    'LOCAL',
    60000000,
    30000000,
    TRUE,
    FALSE
) ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    name = VALUES(name),
    role = VALUES(role),
    provider = VALUES(provider),
    income = VALUES(income),
    asset = VALUES(asset),
    is_shared = VALUES(is_shared),
    is_deleted = VALUES(is_deleted);

INSERT INTO `user` (
    phone,
    password,
    name,
    role,
    provider,
    income,
    asset,
    is_shared,
    is_deleted
) VALUES (
    @home_bride_phone,
    @home_test_password_hash,
    '홈테스트신부',
    'B',
    'LOCAL',
    52000000,
    24000000,
    TRUE,
    FALSE
) ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    name = VALUES(name),
    role = VALUES(role),
    provider = VALUES(provider),
    income = VALUES(income),
    asset = VALUES(asset),
    is_shared = VALUES(is_shared),
    is_deleted = VALUES(is_deleted);

SET @home_groom_id = (
    SELECT id
    FROM `user`
    WHERE phone = @home_groom_phone
    LIMIT 1
);
SET @home_bride_id = (
    SELECT id
    FROM `user`
    WHERE phone = @home_bride_phone
    LIMIT 1
);

INSERT INTO couple (
    groom_id,
    bride_id,
    status
)
SELECT
    @home_groom_id,
    @home_bride_id,
    'CONNECTED'
WHERE NOT EXISTS (
    SELECT 1
    FROM couple
    WHERE groom_id = @home_groom_id
      AND bride_id = @home_bride_id
);

UPDATE couple
SET status = 'CONNECTED'
WHERE groom_id = @home_groom_id
  AND bride_id = @home_bride_id;

SET @home_couple_id = (
    SELECT id
    FROM couple
    WHERE groom_id = @home_groom_id
      AND bride_id = @home_bride_id
    ORDER BY id DESC
    LIMIT 1
);

INSERT INTO goal (
    couple_id,
    name,
    wedding_date,
    region,
    total_budget,
    groom_ratio,
    bride_ratio,
    status,
    is_main
)
SELECT
    @home_couple_id,
    '홈 화면 테스트 대표 목표',
    DATE_ADD(CURRENT_DATE, INTERVAL 8 MONTH),
    '서울',
    50000000,
    50,
    50,
    'NORMAL',
    TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM goal
    WHERE couple_id = @home_couple_id
      AND name = '홈 화면 테스트 대표 목표'
);

UPDATE goal
SET wedding_date = DATE_ADD(CURRENT_DATE, INTERVAL 8 MONTH),
    region = '서울',
    total_budget = 50000000,
    groom_ratio = 50,
    bride_ratio = 50,
    status = 'NORMAL',
    is_main = TRUE
WHERE couple_id = @home_couple_id
  AND name = '홈 화면 테스트 대표 목표';

INSERT INTO account (
    user_id,
    company,
    number,
    card_name,
    connected_id,
    account_id,
    account_password
)
SELECT
    @home_groom_id,
    'KB국민은행',
    11090010001,
    '홈 테스트 계좌',
    'home-test-account-connected',
    'home-test-account',
    '0000'
WHERE NOT EXISTS (
    SELECT 1
    FROM account
    WHERE user_id = @home_groom_id
      AND connected_id = 'home-test-account-connected'
);

UPDATE account
SET company = 'KB국민은행',
    number = 11090010001,
    card_name = '홈 테스트 계좌',
    account_id = 'home-test-account',
    account_password = '0000'
WHERE user_id = @home_groom_id
  AND connected_id = 'home-test-account-connected';

SET @home_account_id = (
    SELECT id
    FROM account
    WHERE user_id = @home_groom_id
      AND connected_id = 'home-test-account-connected'
    ORDER BY id DESC
    LIMIT 1
);

INSERT INTO card (
    user_id,
    card_product_id,
    company,
    number,
    card_name,
    connected_id,
    card_id,
    card_password
)
SELECT
    @home_groom_id,
    NULL,
    'KB국민카드',
    9412900100010001,
    '홈 테스트 카드',
    'home-test-card-connected',
    'home-test-card',
    '0000'
WHERE NOT EXISTS (
    SELECT 1
    FROM card
    WHERE user_id = @home_groom_id
      AND connected_id = 'home-test-card-connected'
);

UPDATE card
SET company = 'KB국민카드',
    number = 9412900100010001,
    card_name = '홈 테스트 카드',
    card_id = 'home-test-card',
    card_password = '0000'
WHERE user_id = @home_groom_id
  AND connected_id = 'home-test-card-connected';

INSERT INTO account_record (
    account_id,
    type,
    category,
    title,
    amount,
    record_date,
    memo
)
SELECT
    @home_account_id,
    'SAVING',
    'WEDDING',
    '홈 테스트 첫 저축',
    1000000,
    CURRENT_DATE,
    '홈 체크리스트 첫 저축 완료 검증용'
WHERE NOT EXISTS (
    SELECT 1
    FROM account_record
    WHERE account_id = @home_account_id
      AND type = 'SAVING'
      AND title = '홈 테스트 첫 저축'
);

UPDATE account_record
SET category = 'WEDDING',
    amount = 1000000,
    record_date = CURRENT_DATE,
    memo = '홈 체크리스트 첫 저축 완료 검증용'
WHERE account_id = @home_account_id
  AND type = 'SAVING'
  AND title = '홈 테스트 첫 저축';

INSERT INTO saving_missions (
    user_id,
    category,
    title,
    expected_saving,
    status,
    created_at
)
SELECT
    @home_groom_id,
    'CAFE',
    '카페 커피 줄이기',
    45000,
    '도전중',
    STR_TO_DATE(CONCAT(DATE_FORMAT(CURRENT_DATE, '%Y-%m'), '-10 09:00:00'), '%Y-%m-%d %H:%i:%s')
WHERE NOT EXISTS (
    SELECT 1
    FROM saving_missions
    WHERE user_id = @home_groom_id
      AND title = '카페 커피 줄이기'
);

UPDATE saving_missions
SET category = 'CAFE',
    expected_saving = 45000,
    status = '도전중',
    created_at = STR_TO_DATE(CONCAT(DATE_FORMAT(CURRENT_DATE, '%Y-%m'), '-10 09:00:00'), '%Y-%m-%d %H:%i:%s')
WHERE user_id = @home_groom_id
  AND title = '카페 커피 줄이기';

INSERT INTO saving_missions (
    user_id,
    category,
    title,
    expected_saving,
    status,
    created_at
)
SELECT
    @home_groom_id,
    'DELIVERY',
    '배달 대신 집밥하기',
    28000,
    '도전중',
    STR_TO_DATE(CONCAT(DATE_FORMAT(CURRENT_DATE, '%Y-%m'), '-12 09:00:00'), '%Y-%m-%d %H:%i:%s')
WHERE NOT EXISTS (
    SELECT 1
    FROM saving_missions
    WHERE user_id = @home_groom_id
      AND title = '배달 대신 집밥하기'
);

UPDATE saving_missions
SET category = 'DELIVERY',
    expected_saving = 28000,
    status = '도전중',
    created_at = STR_TO_DATE(CONCAT(DATE_FORMAT(CURRENT_DATE, '%Y-%m'), '-12 09:00:00'), '%Y-%m-%d %H:%i:%s')
WHERE user_id = @home_groom_id
  AND title = '배달 대신 집밥하기';

INSERT INTO saving_missions (
    user_id,
    category,
    title,
    expected_saving,
    status,
    created_at
)
SELECT
    @home_groom_id,
    'SHOPPING',
    '충동구매 참기',
    50000,
    '완료',
    STR_TO_DATE(CONCAT(DATE_FORMAT(CURRENT_DATE, '%Y-%m'), '-11 09:00:00'), '%Y-%m-%d %H:%i:%s')
WHERE NOT EXISTS (
    SELECT 1
    FROM saving_missions
    WHERE user_id = @home_groom_id
      AND title = '충동구매 참기'
);

UPDATE saving_missions
SET category = 'SHOPPING',
    expected_saving = 50000,
    status = '완료',
    created_at = STR_TO_DATE(CONCAT(DATE_FORMAT(CURRENT_DATE, '%Y-%m'), '-11 09:00:00'), '%Y-%m-%d %H:%i:%s')
WHERE user_id = @home_groom_id
  AND title = '충동구매 참기';

INSERT INTO saving_missions (
    user_id,
    category,
    title,
    expected_saving,
    status,
    created_at
)
SELECT
    @home_groom_id,
    'TRANSPORT',
    '지난달 대중교통 미션',
    10000,
    '도전중',
    DATE_SUB(
        STR_TO_DATE(CONCAT(DATE_FORMAT(CURRENT_DATE, '%Y-%m'), '-10 09:00:00'), '%Y-%m-%d %H:%i:%s'),
        INTERVAL 1 MONTH
    )
WHERE NOT EXISTS (
    SELECT 1
    FROM saving_missions
    WHERE user_id = @home_groom_id
      AND title = '지난달 대중교통 미션'
);

UPDATE saving_missions
SET category = 'TRANSPORT',
    expected_saving = 10000,
    status = '도전중',
    created_at = DATE_SUB(
        STR_TO_DATE(CONCAT(DATE_FORMAT(CURRENT_DATE, '%Y-%m'), '-10 09:00:00'), '%Y-%m-%d %H:%i:%s'),
        INTERVAL 1 MONTH
    )
WHERE user_id = @home_groom_id
  AND title = '지난달 대중교통 미션';

SELECT
    @home_groom_id AS login_user_id,
    @home_bride_id AS partner_user_id,
    @home_couple_id AS couple_id,
    @home_groom_phone AS login_phone,
    @home_test_password AS login_password,
    73000 AS expected_current_month_in_progress_amount,
    2 AS expected_in_progress_mission_count,
    1 AS expected_completed_mission_count;
