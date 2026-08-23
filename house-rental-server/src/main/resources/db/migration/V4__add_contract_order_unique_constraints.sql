ALTER TABLE `lease_contract`
  ADD UNIQUE KEY `uk_lease_contract_application` (`application_id`);

ALTER TABLE `lease_order`
  ADD UNIQUE KEY `uk_lease_order_contract` (`contract_id`);
