-- 개선사항 단일 컬럼 4개 → improvements JSON 배열로 교체

ALTER TABLE report
    ADD COLUMN improvements JSON NULL COMMENT '개선 필요 영역 목록 (최대 2개)';

ALTER TABLE report
    DROP COLUMN improvement_category,
    DROP COLUMN improvement_time_label,
    DROP COLUMN improvement_detail,
    DROP COLUMN improvement_tips;
