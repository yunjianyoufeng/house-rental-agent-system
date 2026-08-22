package com.rental.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RentalApplicationAddDTO {

    @NotNull(message = "房源ID不能为空")
    private Long houseId;

    private Long tenantId;

    private Long landlordId;

    private String remark;
}
