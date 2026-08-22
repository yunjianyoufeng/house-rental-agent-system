package com.rental.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HouseStatusUpdateDTO {

    @NotNull(message = "房源状态不能为空")
    private Integer status;
}
