package com.rental.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LeaseOrderCreateDTO {

    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    private Long tenantId;

    /**
     * 兼容旧客户端保留；实际订单金额始终由后端根据合同计算。
     */
    private BigDecimal amount;
}
