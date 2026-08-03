-- 카드 전략 추천 API 로컬 검증용 더미 데이터입니다.
-- 공통 DDL과 애플리케이션 로직은 수정하지 않고, 선택된 로컬 DB에 테스트 데이터만 추가합니다.

SET NAMES utf8mb4;
USE durimoadb;

SET @strategy_groom_phone = '01090010001';
SET @strategy_bride_phone = '01090010002';
SET @strategy_test_password = 'password123';
SET @strategy_test_password_hash = '$2a$10$OJneddDFnJ033zAO1pjLjOpg2SsYo70VannDag89uMh22503jmnl6';

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
    @strategy_groom_phone,
    @strategy_test_password_hash,
    '전략테스트신랑',
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
    @strategy_bride_phone,
    @strategy_test_password_hash,
    '전략테스트신부',
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

SET @strategy_groom_id = (
    SELECT id
    FROM `user`
    WHERE phone = @strategy_groom_phone
    LIMIT 1
);
SET @strategy_bride_id = (
    SELECT id
    FROM `user`
    WHERE phone = @strategy_bride_phone
    LIMIT 1
);

INSERT INTO couple (
    groom_id,
    bride_id,
    status
)
SELECT
    @strategy_groom_id,
    @strategy_bride_id,
    'CONNECTED'
WHERE NOT EXISTS (
    SELECT 1
    FROM couple
    WHERE groom_id = @strategy_groom_id
      AND bride_id = @strategy_bride_id
);

UPDATE couple
SET status = 'CONNECTED'
WHERE groom_id = @strategy_groom_id
  AND bride_id = @strategy_bride_id;

SET @strategy_couple_id = (
    SELECT id
    FROM couple
    WHERE groom_id = @strategy_groom_id
      AND bride_id = @strategy_bride_id
    ORDER BY id DESC
    LIMIT 1
);

INSERT INTO card_product (
    company,
    name,
    annual_fee,
    image,
    benefit,
    `condition`
)
SELECT
    'KB국민',
    '두리모아 웨딩 2%',
    10000,
    'https://example.com/cards/kb-durimoa-wedding.png',
    JSON_OBJECT(
        'benefits',
        JSON_ARRAY(
            JSON_OBJECT(
                'title', '웨딩 생활 2% 적립',
                'description', '전월 실적 30만원 이상 결제금액 2% 적립',
                'benefit_rate', 0.02,
                'monthly_limit_amount', 20000,
                'minimum_performance_amount', 300000
            )
        )
    ),
    '전월 실적 300,000원 이상'
WHERE NOT EXISTS (
    SELECT 1
    FROM card_product
    WHERE company = 'KB국민'
      AND name = '두리모아 웨딩 2%'
);

UPDATE card_product
SET annual_fee = 10000,
    image = 'https://example.com/cards/kb-durimoa-wedding.png',
    benefit = JSON_OBJECT(
        'benefits',
        JSON_ARRAY(
            JSON_OBJECT(
                'title', '웨딩 생활 2% 적립',
                'description', '전월 실적 30만원 이상 결제금액 2% 적립',
                'benefit_rate', 0.02,
                'monthly_limit_amount', 20000,
                'minimum_performance_amount', 300000
            )
        )
    ),
    `condition` = '전월 실적 300,000원 이상',
    updated_at = CURRENT_TIMESTAMP
WHERE company = 'KB국민'
  AND name = '두리모아 웨딩 2%';

INSERT INTO card_product (
    company,
    name,
    annual_fee,
    image,
    benefit,
    `condition`
)
SELECT
    '신한',
    '두리모아 데이트 3%',
    12000,
    'https://example.com/cards/shinhan-durimoa-date.png',
    JSON_OBJECT(
        'benefits',
        JSON_ARRAY(
            JSON_OBJECT(
                'title', '데이트 소비 3% 캐시백',
                'description', '전월 실적 20만원 이상 결제금액 3% 캐시백',
                'benefit_rate', 0.03,
                'monthly_limit_amount', 15000,
                'minimum_performance_amount', 200000
            )
        )
    ),
    '전월 실적 200,000원 이상'
WHERE NOT EXISTS (
    SELECT 1
    FROM card_product
    WHERE company = '신한'
      AND name = '두리모아 데이트 3%'
);

UPDATE card_product
SET annual_fee = 12000,
    image = 'https://example.com/cards/shinhan-durimoa-date.png',
    benefit = JSON_OBJECT(
        'benefits',
        JSON_ARRAY(
            JSON_OBJECT(
                'title', '데이트 소비 3% 캐시백',
                'description', '전월 실적 20만원 이상 결제금액 3% 캐시백',
                'benefit_rate', 0.03,
                'monthly_limit_amount', 15000,
                'minimum_performance_amount', 200000
            )
        )
    ),
    `condition` = '전월 실적 200,000원 이상',
    updated_at = CURRENT_TIMESTAMP
WHERE company = '신한'
  AND name = '두리모아 데이트 3%';

SET @strategy_self_product_id = (
    SELECT id
    FROM card_product
    WHERE company = 'KB국민'
      AND name = '두리모아 웨딩 2%'
    ORDER BY id DESC
    LIMIT 1
);
SET @strategy_partner_product_id = (
    SELECT id
    FROM card_product
    WHERE company = '신한'
      AND name = '두리모아 데이트 3%'
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
    @strategy_groom_id,
    @strategy_self_product_id,
    'KB국민',
    4129001000100001,
    '신랑 KB 웨딩카드',
    'strategy-test-self-card',
    'strategy-self-card',
    '0000'
WHERE NOT EXISTS (
    SELECT 1
    FROM card
    WHERE user_id = @strategy_groom_id
      AND connected_id = 'strategy-test-self-card'
);

UPDATE card
SET card_product_id = @strategy_self_product_id,
    company = 'KB국민',
    number = 4129001000100001,
    card_name = '신랑 KB 웨딩카드',
    card_id = 'strategy-self-card',
    card_password = '0000'
WHERE user_id = @strategy_groom_id
  AND connected_id = 'strategy-test-self-card';

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
    @strategy_bride_id,
    @strategy_partner_product_id,
    '신한',
    4129001000200002,
    '신부 신한 데이트카드',
    'strategy-test-partner-card',
    'strategy-partner-card',
    '0000'
WHERE NOT EXISTS (
    SELECT 1
    FROM card
    WHERE user_id = @strategy_bride_id
      AND connected_id = 'strategy-test-partner-card'
);

UPDATE card
SET card_product_id = @strategy_partner_product_id,
    company = '신한',
    number = 4129001000200002,
    card_name = '신부 신한 데이트카드',
    card_id = 'strategy-partner-card',
    card_password = '0000'
WHERE user_id = @strategy_bride_id
  AND connected_id = 'strategy-test-partner-card';

SET @strategy_self_card_key = (
    SELECT id
    FROM card
    WHERE user_id = @strategy_groom_id
      AND connected_id = 'strategy-test-self-card'
    ORDER BY id DESC
    LIMIT 1
);
SET @strategy_partner_card_key = (
    SELECT id
    FROM card
    WHERE user_id = @strategy_bride_id
      AND connected_id = 'strategy-test-partner-card'
    ORDER BY id DESC
    LIMIT 1
);

INSERT INTO card_record (
    card_id,
    type,
    category,
    title,
    amount,
    record_date,
    memo
)
SELECT
    @strategy_self_card_key,
    'EXPENSE',
    'LIVING',
    '전략 테스트 신랑 기존 실적',
    250000,
    CURRENT_DATE,
    '카드 추천 현재 월 실적 검증용'
WHERE NOT EXISTS (
    SELECT 1
    FROM card_record
    WHERE card_id = @strategy_self_card_key
      AND title = '전략 테스트 신랑 기존 실적'
);

UPDATE card_record
SET type = 'EXPENSE',
    category = 'LIVING',
    amount = 250000,
    record_date = CURRENT_DATE,
    memo = '카드 추천 현재 월 실적 검증용'
WHERE card_id = @strategy_self_card_key
  AND title = '전략 테스트 신랑 기존 실적';

INSERT INTO card_record (
    card_id,
    type,
    category,
    title,
    amount,
    record_date,
    memo
)
SELECT
    @strategy_partner_card_key,
    'EXPENSE',
    'DATE',
    '전략 테스트 신부 기존 실적',
    180000,
    CURRENT_DATE,
    '카드 추천 현재 월 실적 검증용'
WHERE NOT EXISTS (
    SELECT 1
    FROM card_record
    WHERE card_id = @strategy_partner_card_key
      AND title = '전략 테스트 신부 기존 실적'
);

UPDATE card_record
SET type = 'EXPENSE',
    category = 'DATE',
    amount = 180000,
    record_date = CURRENT_DATE,
    memo = '카드 추천 현재 월 실적 검증용'
WHERE card_id = @strategy_partner_card_key
  AND title = '전략 테스트 신부 기존 실적';

SELECT
    @strategy_groom_id AS login_user_id,
    @strategy_bride_id AS partner_user_id,
    @strategy_couple_id AS couple_id,
    @strategy_groom_phone AS login_phone,
    @strategy_test_password AS login_password,
    @strategy_self_card_key AS self_user_card_key,
    @strategy_partner_card_key AS partner_user_card_key,
    100000 AS request_payment_amount,
    2000 AS expected_self_benefit_amount,
    3000 AS expected_partner_benefit_amount,
    @strategy_partner_card_key AS expected_best_user_card_key;
