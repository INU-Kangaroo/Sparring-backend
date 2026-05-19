-- Drop legacy food recommendation tables no longer referenced in backend code.

DROP TABLE IF EXISTS cluster_food_weight;
DROP TABLE IF EXISTS food_recommendation;
DROP TABLE IF EXISTS user_food_feedback;
DROP TABLE IF EXISTS recommend_food;
