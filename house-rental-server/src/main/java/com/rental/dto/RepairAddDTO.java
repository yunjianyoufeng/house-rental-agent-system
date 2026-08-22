package com.rental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RepairAddDTO {

    @NotNull(message = "房源ID不能为空")
    private Long houseId;

    private Long tenantId;

    private Long landlordId;

    @NotBlank(message = "报修内容不能为空")
    private String content;
}
