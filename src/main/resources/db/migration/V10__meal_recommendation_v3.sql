-- 식단 추천 결과 저장 (v3 FastAPI 연동)

CREATE TABLE meal_recommendation (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    meal_type       VARCHAR(20) NOT NULL,
    meal_target_kcal DECIMAL(8,2),
    recommended_at  DATETIME NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_meal_rec_user_meal_time (user_id, meal_type, recommended_at DESC)
);

CREATE TABLE meal_recommendation_item (
    id                      BIGINT NOT NULL AUTO_INCREMENT,
    meal_recommendation_id  BIGINT NOT NULL,
    rank_order              INT NOT NULL,
    title                   VARCHAR(500) NOT NULL,
    total_kcal              DECIMAL(8,2),
    total_carbs             DECIMAL(8,2),
    total_protein           DECIMAL(8,2),
    total_fat               DECIMAL(8,2),
    total_sodium            DECIMAL(8,2),
    reasons_json            JSON,
    menus_json              JSON,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_meal_rec_item_rec_id (meal_recommendation_id),
    CONSTRAINT fk_meal_rec_item FOREIGN KEY (meal_recommendation_id)
        REFERENCES meal_recommendation (id) ON DELETE CASCADE
);
