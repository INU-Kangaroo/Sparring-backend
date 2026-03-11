-- Initial schema for production (Flyway)

CREATE TABLE `users` (
  `birth_date` date DEFAULT NULL,
  `is_active` bit(1) NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `last_login_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `profile_image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `gender` enum('FEMALE','MALE','OTHER') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provider` enum('GOOGLE','KAKAO','LOCAL') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `surveys` (
  `is_deleted` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `survey_type` enum('SURVEY') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKhu6r4qqshk7b5w9al5y97axsd` (`survey_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `food` (
  `is_deleted` bit(1) NOT NULL,
  `serving_size` double NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `portion_label` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `serving_unit` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `manufacturer` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `portion_amount` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `exercise` (
  `is_deleted` bit(1) NOT NULL,
  `met_value` double NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `exercise_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` enum('AEROBIC','FLEXIBILITY','STRENGTH') COLLATE utf8mb4_unicode_ci NOT NULL,
  `impact_level` enum('HIGH_IMPACT','LOW_IMPACT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `intensity` enum('HIGH','LOW','MEDIUM') COLLATE utf8mb4_unicode_ci NOT NULL,
  `location` enum('GYM','INDOOR','OUTDOOR') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_exercise_category` (`category`),
  KEY `idx_exercise_intensity` (`intensity`),
  KEY `idx_exercise_location` (`location`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `recommendation` (
  `is_deleted` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `recommendation_id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `filter_duration` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `filter_intensity` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `filter_location` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` enum('EXERCISE','MEAL','SUPPLEMENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`recommendation_id`),
  KEY `FKrwu8ddeus85k2p40wlvojuw1v` (`user_id`),
  CONSTRAINT `FKrwu8ddeus85k2p40wlvojuw1v` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `questions` (
  `is_deleted` bit(1) NOT NULL,
  `is_required` bit(1) NOT NULL,
  `question_order` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `survey_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `health_profile_field` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `question_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `options` text COLLATE utf8mb4_unicode_ci,
  `question_text` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `question_stage` enum('BASIC','DETAILED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `question_type` enum('MULTIPLE_CHOICE','NUMBER','SINGLE_CHOICE','TEXT') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK71hl2hwwce3p09skojucrddhf` (`survey_id`,`question_key`,`question_stage`),
  CONSTRAINT `FKnf38uiy78c0g0tmo68btk3y0p` FOREIGN KEY (`survey_id`) REFERENCES `surveys` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `answers` (
  `is_deleted` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `answer_text` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKnuhasayngavi95ujpmx6bk8np` (`user_id`,`question_id`),
  KEY `FK3erw1a3t0r78st8ty27x6v3g1` (`question_id`),
  CONSTRAINT `FK3erw1a3t0r78st8ty27x6v3g1` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`),
  CONSTRAINT `FK5bp3d5loftq2vjn683ephn75a` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `health_profile` (
  `avg_steps` int DEFAULT NULL,
  `birth_date` date DEFAULT NULL,
  `bmi` decimal(4,2) DEFAULT NULL,
  `caffeine_intake` bit(1) DEFAULT NULL,
  `has_family_hypertension` bit(1) DEFAULT NULL,
  `height` decimal(5,2) DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `sleep_hours` decimal(4,1) DEFAULT NULL,
  `smoking_status` bit(1) DEFAULT NULL,
  `weight` decimal(5,2) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `health_goal` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `allergies` text COLLATE utf8mb4_unicode_ci,
  `exercise_place` json DEFAULT NULL,
  `exercise_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `food_preference` json DEFAULT NULL,
  `medications` text COLLATE utf8mb4_unicode_ci,
  `blood_pressure_status` enum('BORDERLINE','NORMAL','STAGE1','STAGE2','UNKNOWN') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `blood_sugar_status` enum('BORDERLINE','NORMAL','TYPE1','TYPE2','UNKNOWN') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `drinking_frequency` enum('NONE','ONE_TO_TWO_PER_WEEK','THREE_OR_MORE_PER_WEEK') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `exercise_duration` enum('LONG','MEDIUM','ONE_TO_TWO_H','OVER_3H','SHORT','TWO_TO_THREE_H','UNDER_1H') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `exercise_frequency` enum('FOUR_TO_FIVE','ONE_TO_TWO','THREE_TO_FOUR','TWO_TO_THREE','ZERO') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `gender` enum('FEMALE','MALE','OTHER') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `meal_frequency` enum('FOUR_TO_FIVE','ONE_TO_TWO','THREE_TO_FOUR','TWO_TO_THREE','ZERO') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sleep_quality` enum('BAD','GOOD','NORMAL') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `stress_level` enum('HIGH','LOW','MEDIUM') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sugar_intake_freq` enum('DAILY','FIVE_TO_SIX_PER_WEEK','NONE','ONE_TO_TWO_PER_WEEK','THREE_TO_FOUR_PER_WEEK') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `FKi5cnr23siyeibdnnk66t1cqmq` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `blood_pressure_log` (
  `diastolic` int NOT NULL,
  `heart_rate` int DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `systolic` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `measured_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `measurement_label` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKfmhobu8f48s71vtcylymbf9q6` (`user_id`),
  CONSTRAINT `FKfmhobu8f48s71vtcylymbf9q6` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `blood_pressure_prediction` (
  `is_deleted` bit(1) NOT NULL,
  `predicted_diastolic` int NOT NULL,
  `predicted_systolic` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `predicted_at` datetime(6) NOT NULL,
  `target_datetime` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqc38n071juxkjbjksr8kwxs80` (`user_id`),
  CONSTRAINT `FKqc38n071juxkjbjksr8kwxs80` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `blood_sugar_log` (
  `glucose_level` int NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `measurement_time` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `measurement_label` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK1jf4hxcoq8qhfr16ickv9jxvl` (`user_id`),
  CONSTRAINT `FK1jf4hxcoq8qhfr16ickv9jxvl` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `blood_sugar_prediction` (
  `confidence_score` decimal(5,2) DEFAULT NULL,
  `is_data_insufficient` bit(1) NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `predicted_value` decimal(5,2) NOT NULL,
  `prediction_date` date NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `risk_level` enum('HIGH','LOW','MEDIUM','VERY_HIGH') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trend_label` enum('DECREASING','INCREASING','STABLE') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbc5m4lhsdmosampur7v84hyla` (`user_id`),
  CONSTRAINT `FKbc5m4lhsdmosampur7v84hyla` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `exercise_log` (
  `calories_burned` double NOT NULL,
  `duration_minutes` int NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `met_value` double NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `logged_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `exercise_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `matched_exercise_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_exercise_log_user_logged_at` (`user_id`,`logged_at`),
  CONSTRAINT `FK6uf3o8nhuo5qsvqkunt29ahda` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `exercise_recommendation` (
  `is_deleted` bit(1) NOT NULL,
  `max_calories` int DEFAULT NULL,
  `min_calories` int DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recommendation_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `duration` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `frequency` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `precautions` text COLLATE utf8mb4_unicode_ci,
  `exercise_type` enum('CARDIAC','STRENGTH') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKtrmtbasl8cihoqtprka327cef` (`recommendation_id`),
  CONSTRAINT `FKtrmtbasl8cihoqtprka327cef` FOREIGN KEY (`recommendation_id`) REFERENCES `recommendation` (`recommendation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `meal_log` (
  `calories` double DEFAULT NULL,
  `carbs` double DEFAULT NULL,
  `eaten_amount_gram` double DEFAULT NULL,
  `fat` double DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `protein` double DEFAULT NULL,
  `sodium` double DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `eaten_at` datetime(6) NOT NULL,
  `food_id` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `food_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `meal_time` enum('BREAKFAST','DINNER','LUNCH','SNACK') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKkqw85oloaff75i93ylgev7u8q` (`food_id`),
  KEY `FKn5c1ptqo5uimm82ukrmlaerk9` (`user_id`),
  CONSTRAINT `FKkqw85oloaff75i93ylgev7u8q` FOREIGN KEY (`food_id`) REFERENCES `food` (`id`),
  CONSTRAINT `FKn5c1ptqo5uimm82ukrmlaerk9` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `meal_nutrition` (
  `calories` double NOT NULL,
  `carbs` double NOT NULL,
  `cholesterol` double DEFAULT NULL,
  `fat` double NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `protein` double NOT NULL,
  `saturated_fat` double DEFAULT NULL,
  `sodium` double DEFAULT NULL,
  `sugar` double DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `food_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`food_id`),
  CONSTRAINT `FKluhncapbqpis67i34rcsqp3h1` FOREIGN KEY (`food_id`) REFERENCES `food` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `report` (
  `blood_pressure_record_days` int NOT NULL,
  `blood_sugar_record_days` int NOT NULL,
  `end_date` date NOT NULL,
  `health_management_score` int NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `lifestyle_score` int NOT NULL,
  `measurement_consistency_score` int NOT NULL,
  `overall_score` int NOT NULL,
  `record_days` int NOT NULL,
  `start_date` date NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `improvement_category` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `improvement_time_label` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `improvement_detail` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ai_comment` text COLLATE utf8mb4_unicode_ci,
  `improvement_tips` json DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_user_start_deleted` (`user_id`,`start_date`,`is_deleted`),
  KEY `idx_report_user_start` (`user_id`,`start_date`),
  CONSTRAINT `FKq50wsn94sc3mi90gtidk0k34a` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `supplement_recommendation` (
  `is_deleted` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recommendation_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `dosage` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `frequency` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `benefits` text COLLATE utf8mb4_unicode_ci,
  `precautions` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `FKriecnugxroak7ceho7dwv3q7p` (`recommendation_id`),
  CONSTRAINT `FKriecnugxroak7ceho7dwv3q7p` FOREIGN KEY (`recommendation_id`) REFERENCES `recommendation` (`recommendation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
