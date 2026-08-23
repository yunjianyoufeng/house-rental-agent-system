-- 房屋租赁系统初始表结构。
-- 已有数据的数据库通过 baseline-on-migrate 在版本 1 接管，不会执行本文件。
-- 全新空数据库会执行本文件；演示数据仍由根目录 house_rental.sql 单独维护。

CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像',
  `role_code` VARCHAR(20) NOT NULL COMMENT '角色编码：ADMIN/LANDLORD/TENANT',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1正常 0禁用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统用户表';

CREATE TABLE `house` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(100) NOT NULL COMMENT '房源标题',
  `address` VARCHAR(255) NOT NULL COMMENT '详细地址',
  `city` VARCHAR(50) DEFAULT NULL COMMENT '城市',
  `area` VARCHAR(50) DEFAULT NULL COMMENT '区域',
  `rent_price` DECIMAL(10, 2) NOT NULL COMMENT '月租金',
  `deposit` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '押金',
  `house_type` VARCHAR(50) DEFAULT NULL COMMENT '户型',
  `square` DECIMAL(10, 2) DEFAULT NULL COMMENT '面积',
  `floor` VARCHAR(50) DEFAULT NULL COMMENT '楼层',
  `description` TEXT NULL COMMENT '房源描述',
  `image_urls` LONGTEXT NULL COMMENT '房源图片',
  `longitude` DECIMAL(10, 6) DEFAULT NULL COMMENT '经度',
  `latitude` DECIMAL(10, 6) DEFAULT NULL COMMENT '纬度',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1上架 2待支付 3已出租 0下架',
  `audit_status` TINYINT DEFAULT 0 COMMENT '审核状态：0待审核 1通过 2拒绝',
  `publisher_id` BIGINT NOT NULL COMMENT '发布者ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_house_publisher_id` (`publisher_id`),
  KEY `idx_house_audit_status` (`audit_status`),
  KEY `idx_house_status` (`status`),
  CONSTRAINT `fk_house_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='房源表';

CREATE TABLE `appointment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `house_id` BIGINT NOT NULL COMMENT '房源ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租客ID',
  `landlord_id` BIGINT NOT NULL COMMENT '出租者ID',
  `appointment_time` DATETIME NOT NULL COMMENT '预约看房时间',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0待处理 1已同意 2已拒绝',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_appointment_tenant_id` (`tenant_id`),
  KEY `idx_appointment_landlord_id` (`landlord_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='看房预约表';

CREATE TABLE `complaint` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '投诉人ID',
  `target_id` BIGINT DEFAULT NULL COMMENT '被投诉对象ID',
  `content` VARCHAR(255) NOT NULL COMMENT '投诉内容',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0待处理 1已处理',
  `result` VARCHAR(255) DEFAULT NULL COMMENT '处理结果',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_complaint_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='投诉表';

CREATE TABLE `notice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(100) NOT NULL COMMENT '公告标题',
  `content` TEXT NOT NULL COMMENT '公告内容',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1启用 0停用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公告表';

CREATE TABLE `recommend_eval_query` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `query_text` VARCHAR(500) NOT NULL COMMENT '测试查询语句',
  `scene_type` VARCHAR(100) DEFAULT NULL COMMENT '场景类型',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_scene_type` (`scene_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='推荐实验查询表';

CREATE TABLE `recommend_eval_label` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `query_id` BIGINT NOT NULL COMMENT '推荐实验查询ID',
  `house_id` BIGINT NOT NULL COMMENT '相关房源ID',
  `relevance_score` INT DEFAULT 1 COMMENT '相关性评分：1相关，2较相关，3非常相关',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_query_id` (`query_id`),
  KEY `idx_house_id` (`house_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='推荐实验人工标注表';

CREATE TABLE `recommend_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID，可为空',
  `query_text` VARCHAR(500) NOT NULL COMMENT '用户输入的租房需求',
  `model_type` VARCHAR(50) NOT NULL COMMENT '推荐模型类型',
  `result_house_ids` VARCHAR(500) DEFAULT NULL COMMENT '推荐房源ID列表',
  `response_time_ms` INT DEFAULT NULL COMMENT '接口响应时间，单位毫秒',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_model_type` (`model_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='智能推荐记录表';

CREATE TABLE `rental_application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `house_id` BIGINT NOT NULL COMMENT '房源ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租客ID',
  `landlord_id` BIGINT NOT NULL COMMENT '出租者ID',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0待处理 1已同意 2已拒绝',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_rental_application_house_id` (`house_id`),
  KEY `idx_rental_application_tenant_id` (`tenant_id`),
  KEY `idx_rental_application_landlord_id` (`landlord_id`),
  CONSTRAINT `fk_rental_application_house` FOREIGN KEY (`house_id`) REFERENCES `house` (`id`),
  CONSTRAINT `fk_rental_application_landlord` FOREIGN KEY (`landlord_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_rental_application_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租房申请表';

CREATE TABLE `lease_contract` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `house_id` BIGINT NOT NULL COMMENT '房源ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租客ID',
  `landlord_id` BIGINT NOT NULL COMMENT '出租者ID',
  `application_id` BIGINT DEFAULT NULL COMMENT '租房申请ID',
  `start_date` DATE NOT NULL COMMENT '合同开始日期',
  `end_date` DATE NOT NULL COMMENT '合同结束日期',
  `monthly_rent` DECIMAL(10, 2) NOT NULL COMMENT '月租金',
  `deposit` DECIMAL(10, 2) NOT NULL COMMENT '押金',
  `contract_url` VARCHAR(255) DEFAULT NULL COMMENT '合同文件地址',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0待生效 1生效中 2已结束 3已取消',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_lease_contract_tenant_id` (`tenant_id`),
  KEY `idx_lease_contract_landlord_id` (`landlord_id`),
  KEY `idx_lease_contract_application_id` (`application_id`),
  KEY `idx_lease_contract_house_id` (`house_id`),
  CONSTRAINT `fk_lease_contract_application` FOREIGN KEY (`application_id`) REFERENCES `rental_application` (`id`),
  CONSTRAINT `fk_lease_contract_house` FOREIGN KEY (`house_id`) REFERENCES `house` (`id`),
  CONSTRAINT `fk_lease_contract_landlord` FOREIGN KEY (`landlord_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_lease_contract_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租赁合同表';

CREATE TABLE `lease_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `contract_id` BIGINT NOT NULL COMMENT '合同ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租客ID',
  `amount` DECIMAL(10, 2) NOT NULL COMMENT '订单金额',
  `pay_type` VARCHAR(20) DEFAULT NULL COMMENT '支付方式',
  `pay_status` TINYINT DEFAULT 0 COMMENT '支付状态：0未支付 1已支付 2已取消 3已过期',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_lease_order_tenant_id` (`tenant_id`),
  KEY `idx_lease_order_contract_id` (`contract_id`),
  CONSTRAINT `fk_lease_order_contract` FOREIGN KEY (`contract_id`) REFERENCES `lease_contract` (`id`),
  CONSTRAINT `fk_lease_order_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租赁订单表';

CREATE TABLE `repair_request` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `house_id` BIGINT NOT NULL COMMENT '房源ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租客ID',
  `landlord_id` BIGINT NOT NULL COMMENT '出租者ID',
  `content` VARCHAR(255) NOT NULL COMMENT '报修内容',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0待处理 1处理中 2已完成',
  `result` VARCHAR(255) DEFAULT NULL COMMENT '处理结果',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_repair_request_tenant_id` (`tenant_id`),
  KEY `idx_repair_request_landlord_id` (`landlord_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报修表';

CREATE TABLE `user_rental_preference` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户长期租房偏好表';
