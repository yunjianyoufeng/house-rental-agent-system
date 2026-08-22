package com.rental.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentAddDTO {

    @NotNull(message = "房源ID不能为空")
    private Long houseId;

    private Long tenantId;

    private Long landlordId;

    @NotNull(message = "预约时间不能为空")
    private LocalDateTime appointmentTime;

    private String remark;
}
