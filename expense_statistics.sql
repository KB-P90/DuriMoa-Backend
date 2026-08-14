-- =========================================================
-- [1인 가구 월평균 소비지출 통계]
-- 기준연도: 2024년 연간
-- 대상: 전국 1인 가구 / 전체가구
-- 출처: 국가데이터처 「가계동향조사」 / KOSIS
-- 통계표 ID: DT_1L9U005
-- 단위: 원
-- =========================================================

INSERT INTO expense_statistics
(
    age_group,
    household_type,
    year,
    category_code,
    category_name,
    average_amount,
    source,
    source_table_id
)
VALUES
    ('ALL', 'SINGLE', 2024, '01', '식료품·비주류음료', 211778, 'KOSIS', 'DT_1L9U005'),
    ('ALL', 'SINGLE', 2024, '02', '주류·담배', 34545, 'KOSIS', 'DT_1L9U005'),
    ('ALL', 'SINGLE', 2024, '03', '의류·신발', 80193, 'KOSIS', 'DT_1L9U005'),
    ('ALL', 'SINGLE', 2024, '04', '주거·수도·광열', 311139, 'KOSIS', 'DT_1L9U005'),
    ('ALL', 'SINGLE', 2024, '05', '가정용품·가사서비스', 66239, 'KOSIS', 'DT_1L9U005'),
    ('ALL', 'SINGLE', 2024, '06', '보건', 140006, 'KOSIS', 'DT_1L9U005'),
    ('ALL', 'SINGLE', 2024, '07', '교통', 179472, 'KOSIS', 'DT_1L9U005'),
    ('ALL', 'SINGLE', 2024, '08', '통신', 70131, 'KOSIS', 'DT_1L9U005'),
    ('ALL', 'SINGLE', 2024, '09', '오락·문화', 135596, 'KOSIS', 'DT_1L9U005'),
    ('ALL', 'SINGLE', 2024, '10', '교육', 23803, 'KOSIS', 'DT_1L9U005'),
    ('ALL', 'SINGLE', 2024, '11', '음식·숙박', 306756, 'KOSIS', 'DT_1L9U005'),
    ('ALL', 'SINGLE', 2024, '12', '기타상품·서비스', 129049, 'KOSIS', 'DT_1L9U005');