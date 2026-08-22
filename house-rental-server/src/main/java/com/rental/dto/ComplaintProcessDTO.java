package com.rental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ComplaintProcessDTO {

    @NotNull(message = "状态不能为空")
    private Integer status;

    @NotBlank(message = "处理结果不能为空")
    private String result;
}