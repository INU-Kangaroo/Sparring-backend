-- 식단 추천 새로고침 nonce 추적 컬럼 추가

ALTER TABLE meal_recommendation
    ADD COLUMN refresh_nonce VARCHAR(64) NULL AFTER meal_target_kcal;
