package com.rental.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserRentalPreferenceUpdateDTO {

    @DecimalMin(value = "0.00", message = "最低预算不能小于0")
    private BigDecimal budgetMin;

    @DecimalMin(value = "0.00", message = "最高预算不能小于0")
    private BigDecimal budgetMax;

    @Size(max = 50, message = "偏好城市不能超过50个字符")
    private String preferredCity;

    @Size(max = 100, message = "偏好区域不能超过100个字符")
    private String preferredArea;

    @Size(max = 50, message = "偏好户型不能超过50个字符")
    private String preferredHouseType;

    @Size(max = 150, message = "工作地点不能超过150个字符")
    private String workplace;

    @Min(value = 1, message = "最大通勤时间必须大于0")
    @Max(value = 300, message = "最大通勤时间不能超过300分钟")
    private Integer maxCommuteMinutes;

    @Size(max = 500, message = "居住偏好不能超过500个字符")
    private String preferenceTags;
}
