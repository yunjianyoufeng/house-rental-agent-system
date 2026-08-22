package com.rental.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LeaseOrderPayDTO {

    @NotBlank(message = "支付方式不能为空")
    private String payType;
}