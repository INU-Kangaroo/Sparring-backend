CREATE TABLE `step_log` (
  `steps` int NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `synced_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `step_date` date NOT NULL,
  `source` enum('APPLE_HEALTH','GOOGLE_FIT','MANUAL') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_step_log_user_date_source_deleted` (`user_id`,`step_date`,`source`,`is_deleted`),
  KEY `idx_step_log_user_step_date` (`user_id`,`step_date`),
  CONSTRAINT `fk_step_log_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
