CREATE TABLE `agent_conversation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` VARCHAR(100) NOT NULL COMMENT '前端会话标识',
  `user_id` BIGINT NOT NULL COMMENT '租客用户ID',
  `title` VARCHAR(100) NOT NULL COMMENT '会话标题',
  `summary` TEXT NULL COMMENT '较早对话的压缩摘要',
  `summary_message_id` BIGINT NOT NULL DEFAULT 0 COMMENT '摘要已覆盖的最后消息ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常 0已归档',
  `last_message_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后消息时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_conversation_user` (`user_id`, `conversation_id`),
  KEY `idx_agent_conversation_last_time` (`user_id`, `last_message_time`),
  CONSTRAINT `fk_agent_conversation_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Agent会话表';

CREATE TABLE `agent_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` VARCHAR(100) NOT NULL COMMENT '前端会话标识',
  `user_id` BIGINT NOT NULL COMMENT '租客用户ID',
  `role` VARCHAR(20) NOT NULL COMMENT '消息角色：user/assistant',
  `content` MEDIUMTEXT NOT NULL COMMENT '消息正文',
  `sources_json` MEDIUMTEXT NULL COMMENT 'RAG来源JSON',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_agent_message_context` (`user_id`, `conversation_id`, `id`),
  CONSTRAINT `fk_agent_message_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Agent消息表';
