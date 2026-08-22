package com.rental.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class HouseAddDTO {

    @NotBlank(message = "房源标题不能为空")
    private String title;

    @NotBlank(message = "详细地址不能为空")
    private String address;

    private String city;

    private String area;

    @NotNull(message = "租金不能为空")
    @DecimalMin(value = "0.01", message = "租金必须大于0")
    private BigDecimal rentPrice;

    @DecimalMin(value = "0.00", message = "押金不能小于0")
    private BigDecimal deposit;

    private String houseType;

    @DecimalMin(value = "0.00", message = "面积不能小于0")
    private BigDecimal square;

    private String floor;

    private String description;

    private String imageUrls;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private Long publisherId;
}
