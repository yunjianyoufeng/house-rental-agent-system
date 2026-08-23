ALTER TABLE `appointment`
  ADD COLUMN `request_key` VARCHAR(100) NULL COMMENT '预约幂等请求键' AFTER `remark`,
  ADD UNIQUE KEY `uk_appointment_tenant_request` (`tenant_id`, `request_key`);
