CREATE TABLE IF NOT EXISTS `user_rental_preference` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '租客用户ID',
    `budget_min` DECIMAL(10, 2) DEFAULT NULL COMMENT '月租最低预算',
    `budget_max` DECIMAL(10, 2) DEFAULT NULL COMMENT '月租最高预算',
    `preferred_city` VARCHAR(50) DEFAULT NULL COMMENT '偏好城市',
    `preferred_area` VARCHAR(100) DEFAULT NULL COMMENT '偏好区域',
    `preferred_house_type` VARCHAR(50) DEFAULT NULL COMMENT '偏好户型',
    `workplace` VARCHAR(150) DEFAULT NULL COMMENT '工作或学习地点',
    `max_commute_minutes` INT DEFAULT NULL COMMENT '最大接受通勤时间（分钟）',
    `preference_tags` VARCHAR(500) DEFAULT NULL COMMENT '居住偏好标签',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_rental_preference_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='用户长期租房偏好';
