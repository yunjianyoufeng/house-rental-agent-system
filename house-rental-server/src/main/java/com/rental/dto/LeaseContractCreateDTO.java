package com.rental.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LeaseContractCreateDTO {

    @NotNull(message = "房源ID不能为空")
    private Long houseId;

    @NotNull(message = "租客ID不能为空")
    private Long tenantId;

    @NotNull(message = "出租者ID不能为空")
    private Long landlordId;

    @NotNull(message = "申请ID不能为空")
    private Long applicationId;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @NotNull(message = "月租金不能为空")
    private BigDecimal monthlyRent;

    @NotNull(message = "押金不能为空")
    private BigDecimal deposit;

    private String contractUrl;
}