package com.rental.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class HouseSearchDTO {

    private String city;

    private String area;

    @PositiveOrZero(message = "最低租金不能小于0")
    private BigDecimal minRent;

    @PositiveOrZero(message = "最高租金不能小于0")
    private BigDecimal maxRent;

    private String houseType;

    private String keyword;

    @Min(value = 1, message = "返回数量不能小于1")
    @Max(value = 20, message = "返回数量不能超过20")
    private Integer limit = 5;
}
