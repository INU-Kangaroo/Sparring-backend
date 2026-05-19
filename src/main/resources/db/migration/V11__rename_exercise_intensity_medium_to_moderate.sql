-- 운동 강도 enum MEDIUM -> MODERATE 전환
-- 1) enum 확장 (MEDIUM + MODERATE 공존)
ALTER TABLE exercise_master
    MODIFY COLUMN intensity enum('HIGH','LOW','MEDIUM','MODERATE')
    COLLATE utf8mb4_unicode_ci NOT NULL;

-- 2) 기존 데이터 치환
UPDATE exercise_master
SET intensity = 'MODERATE'
WHERE intensity = 'MEDIUM';

UPDATE recommendation_session
SET filter_intensity = 'MODERATE'
WHERE filter_intensity = 'MEDIUM';

-- 3) MEDIUM 제거
ALTER TABLE exercise_master
    MODIFY COLUMN intensity enum('HIGH','LOW','MODERATE')
    COLLATE utf8mb4_unicode_ci NOT NULL;
