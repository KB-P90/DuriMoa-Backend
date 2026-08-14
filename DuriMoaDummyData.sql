USE durimoadb;

-- 사용자
INSERT INTO `user` (`id`, `phone`, `password`, `name`, `role`, `provider`, `income`, `asset`, `is_shared`, `is_deleted`)
VALUES (1, '01011111111', '$2b$12$W5pjleiKbB/3F4s1gDOG1.tZDQNUNOIDOVB0i/QVDBRgqPag6LL.e', '유저1', 'G', 'LOCAL', 40000000,
        20000000, TRUE, FALSE),
       (2, '01022222222', '$2b$12$W5pjleiKbB/3F4s1gDOG1.tZDQNUNOIDOVB0i/QVDBRgqPag6LL.e', '유저2', 'B', 'LOCAL', 38000000,
        15000000, TRUE, FALSE),
       (3, '01033333333', '$2b$12$W5pjleiKbB/3F4s1gDOG1.tZDQNUNOIDOVB0i/QVDBRgqPag6LL.e', '유저3', 'G', 'LOCAL', 35000000,
        10000000, FALSE, FALSE),
       (4, '01044444444', '$2b$12$W5pjleiKbB/3F4s1gDOG1.tZDQNUNOIDOVB0i/QVDBRgqPag6LL.e', '유저4', 'B', 'LOCAL', 32000000,
        8000000, FALSE, FALSE);

-- 커플 (유저1=신랑, 유저2=신부)
INSERT INTO `couple` (`id`, `groom_id`, `bride_id`, `status`)
VALUES (1, 1, 2, 'CONNECTED');

-- 목표
INSERT INTO `goal` (`id`, `couple_id`, `name`, `wedding_date`, `region`, `total_budget`, `groom_ratio`, `bride_ratio`,
                    `status`, `is_main`, `requested_by`, `requested_at`)
VALUES (1, 1, '기본 시안', '2027-05-15', '서울', 50000000, 50, 50, 'CONFIRMED', TRUE, NULL, NULL),
       (2, 1, '절약형 시안', '2027-05-15', '서울', 35000000, 60, 40, 'PENDING', FALSE, 1, NOW());

INSERT INTO `goal_item`
(`goal_id`, `category`, `target_amount`, `is_included`, `is_completed`, `type`)
VALUES
-- =========================
-- goal_id = 1 (기본 시안)
-- =========================
(1, 'WEDDINGHALL', 25000000, TRUE, FALSE, '균형형'),
(1, 'STUDIO', 3500000, TRUE, FALSE, '균형형'),
(1, 'MAKEUP', 2000000, TRUE, FALSE, '균형형'),
(1, 'DRESS', 5000000, TRUE, FALSE, '균형형'),
(1, 'JEWELRY', 9000000, TRUE, FALSE, '균형형'),
(1, 'EMERGENCY', 5500000, TRUE, FALSE, '균형형'),
-- =========================
-- goal_id = 2 (절약형 시안)
-- =========================
(2, 'WEDDINGHALL', 17000000, TRUE, FALSE, '알뜰형'),
(2, 'STUDIO', 2500000, TRUE, FALSE, '알뜰형'),
(2, 'MAKEUP', 1500000, TRUE, FALSE, '알뜰형'),
(2, 'DRESS', 3500000, TRUE, FALSE, '알뜰형'),
(2, 'JEWELRY', 5500000, TRUE, FALSE, '알뜰형'),
(2, 'EMERGENCY', 5000000, TRUE, FALSE, '알뜰형');

-- 카드 상품 (card_product)
INSERT INTO `card_product` (`id`, `company`, `name`, `annual_fee`, `image`, `benefit`, `condition`)
VALUES (1, 'KB국민카드', 'KB국민 My WE:SH 카드', 15000, 'https://img1.kbcard.com/card/card_1.png', '[
  "KB Pay 10% 할인",
  "음식점/편의점 10% 할인",
  "미용실/미용 10% 할인"
]', '전월 실적 40만원 이상'),
       (2, 'KB국민카드', 'KB국민 톡톡 My Point 카드', 12000, 'https://img1.kbcard.com/card/card_2.png', '[
         "KB Pay 결제 시 5% 적립",
         "전가맹점 0.5% 적립"
       ]', '전월 실적 조건 없음'),
       (3, '우리카드', '우리카드 카드의정석 Every 1', 12000, 'https://img1.wooricard.com/card/card_3.png', '[
         "전가맹점 1% 적립",
         "최대 2만원 추가 적립"
       ]', '전월 실적 50만원 이상'),
       (4, '신한카드', '신한카드 Mr.Life', 15000, 'https://img1.shinhancard.com/card/card_4.png', '[
         "공과금 10% 할인",
         "마트/편의점/병원 10% 할인"
       ]', '전월 실적 30만원 이상');

-- 사용자 연동 카드 정보 (card)
INSERT INTO `card` (`id`, `user_id`, `card_product_id`, `company`, `number`, `card_name`)
VALUES (1, 1, 1, 'KB국민카드', '5389-2026-0804-0104', 'KB국민 My WE:SH 카드'),
       (2, 1, 3, '우리카드', '5389-2026-0804-5992', '우리카드 카드의정석 Every 1'),
       (3, 2, 2, 'KB국민카드', '5520-2026-0804-1234', 'KB국민 톡톡 My Point 카드');

-- 계좌 정보 (account)
INSERT INTO `account` (`id`, `user_id`, `company`, `number`)
VALUES (1, 1, 'KB국민은행', '123-456-789012'),
       (2, 2, '신한은행', '110-123-456789');

-- 서비스 거래 내역 (service record)
INSERT INTO service_record (user_id, type, category, changed_category, amount, record_date)
VALUES
-- =========================
-- USER 1
-- =========================
(1, 'INCOME', 'SALARY', NULL, 3000000, '2026-07-01'),
(1, 'EXPENSE', 'FOOD', NULL, 12000, '2026-07-01'),
(1, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-07-01'),
(1, 'EXPENSE', 'CAFE', NULL, 5500, '2026-07-02'),
-- 자동 분류 실패 후 사용자 수정
(1, 'EXPENSE', 'ETC', 'WEDDINGHALL', 100000, '2026-07-02'),
(1, 'EXPENSE', 'FOOD', NULL, 18000, '2026-07-03'),
(1, 'EXPENSE', 'SHOPPING', NULL, 45000, '2026-07-03'),
(1, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-07-04'),
(1, 'EXPENSE', 'DRESS', NULL, 350000, '2026-07-04'),
(1, 'EXPENSE', 'FOOD', NULL, 9500, '2026-07-05'),
(1, 'EXPENSE', 'ETC', NULL, 28000, '2026-07-05'),
(1, 'EXPENSE', 'CAFE', NULL, 6200, '2026-07-06'),
(1, 'EXPENSE', 'TRANSPORT', NULL, 1450, '2026-07-06'),
(1, 'EXPENSE', 'ETC', 'JEWELRY', 800000, '2026-07-07'),
(1, 'SAVING', 'GENERALSAVING', NULL, 500000, '2026-07-07'),
(1, 'EXPENSE', 'FOOD', NULL, 13000, '2026-07-08'),
(1, 'EXPENSE', 'CULTURE', NULL, 18000, '2026-07-08'),
(1, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-07-09'),
(1, 'EXPENSE', 'MAKEUP', NULL, 150000, '2026-07-09'),
(1, 'EXPENSE', 'FOOD', NULL, 21000, '2026-07-10'),
(1, 'EXPENSE', 'CAFE', NULL, 4800, '2026-07-10'),
(1, 'EXPENSE', 'ETC', NULL, 55000, '2026-07-11'),
(1, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-07-11'),
(1, 'EXPENSE', 'FOOD', NULL, 17000, '2026-07-12'),
(1, 'EXPENSE', 'ETC', NULL, 22000, '2026-07-12'),
(1, 'EXPENSE', 'STUDIO', NULL, 400000, '2026-07-13'),
(1, 'EXPENSE', 'CAFE', NULL, 5800, '2026-07-13'),
(1, 'EXPENSE', 'TRANSPORT', NULL, 1450, '2026-07-14'),
(1, 'EXPENSE', 'FOOD', NULL, 15000, '2026-07-14'),
(1, 'EXPENSE', 'SHOPPING', NULL, 89000, '2026-07-15'),
(1, 'INCOME', 'SIDEINCOME', NULL, 150000, '2026-07-15'),
(1, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-07-16'),
(1, 'EXPENSE', 'FOOD', NULL, 24000, '2026-07-16'),
(1, 'EXPENSE', 'CAFE', NULL, 5300, '2026-07-17'),
(1, 'EXPENSE', 'CULTURE', NULL, 35000, '2026-07-17'),
(1, 'EXPENSE', 'FOOD', NULL, 11000, '2026-07-18'),
(1, 'EXPENSE', 'ETC', NULL, 41000, '2026-07-18'),
(1, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-07-19'),
(1, 'SAVING', 'GENERALSAVING', NULL, 200000, '2026-07-19'),
(1, 'EXPENSE', 'FOOD', NULL, 19000, '2026-07-20'),
(1, 'EXPENSE', 'CAFE', NULL, 4500, '2026-07-20'),
(1, 'EXPENSE', 'DRESS', NULL, 250000, '2026-07-21'),
(1, 'EXPENSE', 'TRANSPORT', NULL, 1450, '2026-07-21'),
(1, 'EXPENSE', 'FOOD', NULL, 14000, '2026-07-22'),
(1, 'EXPENSE', 'WEDDINGHALL', NULL, 200000, '2026-07-22'),
(1, 'EXPENSE', 'SHOPPING', NULL, 52000, '2026-07-23'),
(1, 'EXPENSE', 'FOOD', NULL, 26000, '2026-07-23'),
(1, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-07-24'),
(1, 'EXPENSE', 'CAFE', NULL, 6100, '2026-07-24'),
(1, 'EXPENSE', 'JEWELRY', NULL, 300000, '2026-07-25');

INSERT INTO service_record (user_id, type, category, changed_category, amount, record_date)
VALUES
-- =========================
-- USER 2
-- =========================
(2, 'INCOME', 'SALARY', NULL, 3600000, '2026-07-01'),
(2, 'EXPENSE', 'FOOD', NULL, 15000, '2026-07-01'),
(2, 'EXPENSE', 'TRANSPORT', NULL, 1450, '2026-07-01'),
(2, 'EXPENSE', 'CAFE', NULL, 6500, '2026-07-02'),
-- 자동 분류 실패 후 사용자 수정
(2, 'EXPENSE', 'ETC', 'WEDDINGHALL', 150000, '2026-07-02'),
(2, 'EXPENSE', 'FOOD', NULL, 21000, '2026-07-03'),
(2, 'EXPENSE', 'SHOPPING', NULL, 67000, '2026-07-03'),
(2, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-07-04'),
(2, 'EXPENSE', 'DRESS', NULL, 280000, '2026-07-04'),
(2, 'EXPENSE', 'FOOD', NULL, 12000, '2026-07-05'),
(2, 'EXPENSE', 'ETC', NULL, 34000, '2026-07-05'),
(2, 'EXPENSE', 'CAFE', NULL, 4900, '2026-07-06'),
(2, 'SAVING', 'GENERALSAVING', NULL, 600000, '2026-07-06'),
-- 사용자가 예물로 변경
(2, 'EXPENSE', 'ETC', 'JEWELRY', 950000, '2026-07-07'),
(2, 'EXPENSE', 'FOOD', NULL, 17000, '2026-07-07'),
(2, 'EXPENSE', 'TRANSPORT', NULL, 1450, '2026-07-08'),
(2, 'EXPENSE', 'MAKEUP', NULL, 180000, '2026-07-08'),
(2, 'EXPENSE', 'CAFE', NULL, 5800, '2026-07-09'),
(2, 'EXPENSE', 'FOOD', NULL, 22000, '2026-07-09'),
(2, 'EXPENSE', 'CULTURE', NULL, 27000, '2026-07-10'),
(2, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-07-10'),
(2, 'EXPENSE', 'STUDIO', NULL, 500000, '2026-07-11'),
(2, 'EXPENSE', 'FOOD', NULL, 18000, '2026-07-11'),
(2, 'EXPENSE', 'ETC', NULL, 35000, '2026-07-12'),
(2, 'EXPENSE', 'CAFE', NULL, 5200, '2026-07-12'),
(2, 'EXPENSE', 'SHOPPING', NULL, 94000, '2026-07-13'),
(2, 'INCOME', 'SIDEINCOME', NULL, 200000, '2026-07-13'),
(2, 'EXPENSE', 'FOOD', NULL, 13000, '2026-07-14'),
(2, 'EXPENSE', 'TRANSPORT', NULL, 1450, '2026-07-14'),
(2, 'EXPENSE', 'ETC', NULL, 26000, '2026-07-15'),
(2, 'EXPENSE', 'FOOD', NULL, 24000, '2026-07-15'),
(2, 'EXPENSE', 'CAFE', NULL, 6900, '2026-07-16'),
(2, 'EXPENSE', 'WEDDINGHALL', NULL, 180000, '2026-07-16'),
(2, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-07-17'),
(2, 'EXPENSE', 'FOOD', NULL, 19000, '2026-07-17'),
(2, 'EXPENSE', 'CULTURE', NULL, 42000, '2026-07-18'),
(2, 'EXPENSE', 'DRESS', NULL, 320000, '2026-07-18'),
(2, 'SAVING', 'GENERALSAVING', NULL, 250000, '2026-07-19'),
(2, 'EXPENSE', 'CAFE', NULL, 4300, '2026-07-20'),
(2, 'EXPENSE', 'FOOD', NULL, 14000, '2026-07-20'),
(2, 'EXPENSE', 'TRANSPORT', NULL, 1450, '2026-07-21'),
(2, 'EXPENSE', 'JEWELRY', NULL, 450000, '2026-07-21'),
(2, 'EXPENSE', 'SHOPPING', NULL, 73000, '2026-07-22'),
(2, 'EXPENSE', 'FOOD', NULL, 20000, '2026-07-22'),
(2, 'EXPENSE', 'MAKEUP', NULL, 120000, '2026-07-23'),
(2, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-07-24'),
(2, 'EXPENSE', 'CAFE', NULL, 5100, '2026-07-24'),
(2, 'EXPENSE', 'FOOD', NULL, 25000, '2026-07-25'),
(2, 'EXPENSE', 'STUDIO', NULL, 350000, '2026-07-26'),
(2, 'EXPENSE', 'ETC', NULL, 31000, '2026-07-27');

INSERT INTO service_record
    (user_id, type, category, changed_category, amount, record_date)
VALUES (1, 'INCOME', 'SALARY', NULL, 3000000, '2026-08-01'),
       (1, 'EXPENSE', 'FOOD', NULL, 12000, '2026-08-01'),
       (1, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-08-01'),
       (1, 'EXPENSE', 'CAFE', NULL, 5500, '2026-08-02'),

       (1, 'EXPENSE', 'ETC', 'WEDDINGHALL', 100000, '2026-08-02'),

       (1, 'EXPENSE', 'FOOD', NULL, 18000, '2026-08-03'),
       (1, 'EXPENSE', 'SHOPPING', NULL, 45000, '2026-08-03'),
       (1, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-08-04');


INSERT INTO service_record
    (user_id, type, category, changed_category, amount, record_date)
VALUES (1, 'INCOME', 'SALARY', NULL, 3000000, '2026-06-01'),

       (1, 'EXPENSE', 'FOOD', NULL, 15000, '2026-06-01'),
       (1, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-06-01'),

       (1, 'EXPENSE', 'CAFE', NULL, 4900, '2026-06-02'),
       (1, 'EXPENSE', 'FOOD', NULL, 17500, '2026-06-02'),

       (1, 'EXPENSE', 'ETC', 'STUDIO', 250000, '2026-06-03'),

       (1, 'EXPENSE', 'TRANSPORT', NULL, 1450, '2026-06-03'),
       (1, 'EXPENSE', 'SHOPPING', NULL, 62000, '2026-06-04'),

       (1, 'EXPENSE', 'FOOD', NULL, 21000, '2026-06-05'),
       (1, 'EXPENSE', 'CAFE', NULL, 5800, '2026-06-05'),

       (1, 'SAVING', 'GENERALSAVING', NULL, 300000, '2026-06-06'),

       (1, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-06-06'),
       (1, 'EXPENSE', 'CULTURE', NULL, 25000, '2026-06-07'),

       (1, 'EXPENSE', 'FOOD', NULL, 14000, '2026-06-07'),
       (1, 'EXPENSE', 'MAKEUP', NULL, 180000, '2026-06-08'),

       (1, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-06-08'),
       (1, 'EXPENSE', 'CAFE', NULL, 5200, '2026-06-09'),

       (1, 'EXPENSE', 'FOOD', NULL, 19000, '2026-06-09'),
       (1, 'EXPENSE', 'ETC', NULL, 35000, '2026-06-10'),

       (1, 'EXPENSE', 'TRANSPORT', NULL, 1450, '2026-06-10'),
       (1, 'EXPENSE', 'FOOD', NULL, 12000, '2026-06-11'),

       (1, 'EXPENSE', 'DRESS', NULL, 300000, '2026-06-11'),

       (1, 'EXPENSE', 'CAFE', NULL, 6300, '2026-06-12'),
       (1, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-06-12'),

       (1, 'EXPENSE', 'FOOD', NULL, 23000, '2026-06-13'),
       (1, 'EXPENSE', 'SHOPPING', NULL, 47000, '2026-06-13'),

       (1, 'EXPENSE', 'JEWELRY', NULL, 450000, '2026-06-14'),

       (1, 'INCOME', 'SIDEINCOME', NULL, 180000, '2026-06-15'),

       (1, 'EXPENSE', 'FOOD', NULL, 13000, '2026-06-15'),
       (1, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-06-16'),

       (1, 'EXPENSE', 'CAFE', NULL, 5400, '2026-06-16'),
       (1, 'EXPENSE', 'CULTURE', NULL, 30000, '2026-06-17'),

       (1, 'EXPENSE', 'FOOD', NULL, 20000, '2026-06-17'),
       (1, 'EXPENSE', 'ETC', NULL, 42000, '2026-06-18'),

       (1, 'EXPENSE', 'TRANSPORT', NULL, 1450, '2026-06-18'),
       (1, 'SAVING', 'GENERALSAVING', NULL, 200000, '2026-06-19'),

       (1, 'EXPENSE', 'FOOD', NULL, 18000, '2026-06-19'),
       (1, 'EXPENSE', 'CAFE', NULL, 4700, '2026-06-20'),

       (1, 'EXPENSE', 'STUDIO', NULL, 350000, '2026-06-20'),

       (1, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-06-21'),
       (1, 'EXPENSE', 'FOOD', NULL, 25000, '2026-06-21'),

       (1, 'EXPENSE', 'WEDDINGHALL', NULL, 250000, '2026-06-22'),

       (1, 'EXPENSE', 'SHOPPING', NULL, 71000, '2026-06-23'),
       (1, 'EXPENSE', 'FOOD', NULL, 16000, '2026-06-23'),

       (1, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-06-24'),
       (1, 'EXPENSE', 'CAFE', NULL, 5900, '2026-06-24'),

       (1, 'EXPENSE', 'FOOD', NULL, 22000, '2026-06-25'),
       (1, 'EXPENSE', 'ETC', NULL, 28000, '2026-06-26'),

       (1, 'EXPENSE', 'TRANSPORT', NULL, 1450, '2026-06-27'),
       (1, 'EXPENSE', 'FOOD', NULL, 17000, '2026-06-27'),

       (1, 'EXPENSE', 'CAFE', NULL, 5100, '2026-06-28'),
       (1, 'EXPENSE', 'SHOPPING', NULL, 54000, '2026-06-28'),

       (1, 'EXPENSE', 'FOOD', NULL, 24000, '2026-06-29'),
       (1, 'EXPENSE', 'TRANSPORT', NULL, 2800, '2026-06-30');